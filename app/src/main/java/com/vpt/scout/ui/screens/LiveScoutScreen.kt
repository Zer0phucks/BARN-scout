package com.vpt.scout.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.vpt.scout.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * State for live scout mode.
 */
enum class ScoutState {
    LOADING_LOCATION,
    FINDING_PROPERTY,
    READY_TO_NAVIGATE,
    NAVIGATING,
    QUESTIONNAIRE,
    SUBMITTING,
    COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScoutScreen(
    propertyRepository: PropertyRepository,
    scoutRepository: ScoutRepository,
    city: String? = null,
    vptOnly: Boolean = false,
    listId: Long? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var scoutState by remember { mutableStateOf(ScoutState.LOADING_LOCATION) }
    var currentProperty by remember { mutableStateOf<Property?>(null) }
    var remainingCount by remember { mutableStateOf(0) }
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var scoutedCount by remember { mutableStateOf(0) }
    
    // Questionnaire
    var followUp by remember { mutableStateOf(false) }
    var flyered by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    
    // Location permission
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            // Re-trigger location fetch
            scoutState = ScoutState.LOADING_LOCATION
        } else {
            error = "Location permission required for scout mode"
        }
    }
    
    // Get current location
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return try {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                locationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
                return null
            }
            
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                // Fallback to Oakland if no location
                Pair(37.8044, -122.2712)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to Oakland
            Pair(37.8044, -122.2712)
        }
    }
    
    // Find next property
    suspend fun findNextProperty() {
        scoutState = ScoutState.FINDING_PROPERTY
        error = null
        
        val location = userLocation ?: getCurrentLocation()
        if (location == null) {
            error = "Could not get your location"
            return
        }
        userLocation = location
        
        try {
            val response = propertyRepository.getNextProperty(
                latitude = location.first,
                longitude = location.second,
                city = city,
                vptOnly = vptOnly,
                listId = listId
            )
            
            if (response.property != null) {
                currentProperty = response.property
                remainingCount = response.remaining
                scoutState = ScoutState.READY_TO_NAVIGATE
            } else {
                scoutState = ScoutState.COMPLETE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error = e.message ?: "Failed to find property"
            scoutState = ScoutState.READY_TO_NAVIGATE
        }
    }
    
    // Navigate to property
    fun navigateToProperty() {
        currentProperty?.let { property ->
            property.latitude?.let { lat ->
                property.longitude?.let { lng ->
                    val uri = Uri.parse("google.navigation:q=$lat,$lng")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    context.startActivity(intent)
                    scoutState = ScoutState.NAVIGATING
                }
            }
        }
    }
    
    // Mark arrived (show questionnaire)
    fun onArrived() {
        scoutState = ScoutState.QUESTIONNAIRE
        followUp = false
        flyered = false
        notes = ""
    }
    
    // Submit questionnaire
    fun submitQuestionnaire() {
        scope.launch {
            scoutState = ScoutState.SUBMITTING
            try {
                currentProperty?.let { property ->
                    scoutRepository.submitScoutResult(
                        apn = property.apn,
                        followUp = followUp,
                        flyered = flyered,
                        notes = notes.takeIf { it.isNotBlank() },
                        latitude = userLocation?.first,
                        longitude = userLocation?.second
                    )
                    scoutedCount++
                }
                // Get fresh location and find next
                userLocation = getCurrentLocation()
                findNextProperty()
            } catch (e: Exception) {
                e.printStackTrace()
                error = e.message ?: "Failed to submit"
                scoutState = ScoutState.QUESTIONNAIRE
            }
        }
    }
    
    // Initial load
    LaunchedEffect(Unit) {
        userLocation = getCurrentLocation()
        if (userLocation != null) {
            findNextProperty()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scout Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Stats chip
                    if (scoutedCount > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("$scoutedCount scouted") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error display
            error?.let { errMsg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        errMsg,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Filters display
            if (city != null || vptOnly || listId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Active filters:", fontWeight = FontWeight.Medium)
                        city?.let { Text(it) }
                        if (vptOnly) Text("VPT")
                        listId?.let { Text("List #$it") }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Main content based on state
            when (scoutState) {
                ScoutState.LOADING_LOCATION -> {
                    LoadingContent("Getting your location...")
                }
                
                ScoutState.FINDING_PROPERTY -> {
                    LoadingContent("Finding nearest property...")
                }
                
                ScoutState.READY_TO_NAVIGATE, ScoutState.NAVIGATING -> {
                    currentProperty?.let { property ->
                        PropertyCard(
                            property = property,
                            remaining = remainingCount,
                            onNavigate = { navigateToProperty() },
                            onArrived = { onArrived() },
                            onSkip = {
                                scope.launch { findNextProperty() }
                            }
                        )
                    }
                }
                
                ScoutState.QUESTIONNAIRE -> {
                    QuestionnaireCard(
                        property = currentProperty,
                        followUp = followUp,
                        onFollowUpChange = { followUp = it },
                        flyered = flyered,
                        onFlyeredChange = { flyered = it },
                        notes = notes,
                        onNotesChange = { notes = it },
                        onSubmit = { submitQuestionnaire() }
                    )
                }
                
                ScoutState.SUBMITTING -> {
                    LoadingContent("Saving result...")
                }
                
                ScoutState.COMPLETE -> {
                    CompleteCard(
                        scoutedCount = scoutedCount,
                        onDone = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
private fun PropertyCard(
    property: Property,
    remaining: Int,
    onNavigate: () -> Unit,
    onArrived: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Property info
            Text(
                property.address ?: property.apn,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                property.city?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                if (property.hasVpt) {
                    Badge(containerColor = Color(0xFFE53935)) {
                        Text("VPT")
                    }
                }
                property.conditionScore?.let { score ->
                    val color = when {
                        score >= 7 -> Color(0xFF4CAF50)
                        score >= 4 -> Color(0xFFFFC107)
                        else -> Color(0xFFE53935)
                    }
                    Badge(containerColor = color) {
                        Text("${"%.0f".format(score)}/10")
                    }
                }
            }
            
            // Distance
            if (property is Property) {
                (property as? Property)?.let { p ->
                    p.latitude?.let { _ ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Distance: nearby",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Remaining count
            Text(
                "$remaining more properties after this",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SkipNext, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Skip")
                }
                
                Button(
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Navigation, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Navigate")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onArrived,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(4.dp))
                Text("I'm Here")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionnaireCard(
    property: Property?,
    followUp: Boolean,
    onFollowUpChange: (Boolean) -> Unit,
    flyered: Boolean,
    onFlyeredChange: (Boolean) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Property Report",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            property?.let {
                Text(
                    it.address ?: it.apn,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Follow Up question
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Needs follow-up?",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = followUp,
                    onCheckedChange = onFollowUpChange
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Flyered question
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Left flyer?",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = flyered,
                    onCheckedChange = onFlyeredChange
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(8.dp))
                Text("Submit & Find Next")
            }
        }
    }
}

@Composable
private fun CompleteCard(
    scoutedCount: Int,
    onDone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "All Done!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "You've scouted $scoutedCount properties",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onDone) {
                Text("Done")
            }
        }
    }
}

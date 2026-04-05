package com.vpt.scout.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.vpt.scout.Property
import com.vpt.scout.ScoutRepository
import com.vpt.scout.proximity.ProximityAlertRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class AlertedScoutUiState {
    LOADING,
    READY,
    SUBMITTING,
    COMPLETE,
    ALREADY_SCOUTED,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertedPropertyScoutScreen(
    apn: String,
    proximityRepository: ProximityAlertRepository,
    scoutRepository: ScoutRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var property by remember(apn) { mutableStateOf<Property?>(null) }
    var uiState by remember(apn) { mutableStateOf(AlertedScoutUiState.LOADING) }
    var errorMessage by remember(apn) { mutableStateOf<String?>(null) }

    suspend fun loadProperty() {
        uiState = AlertedScoutUiState.LOADING
        errorMessage = null
        try {
            val loaded = proximityRepository.getPropertyForAlert(apn)
            when {
                loaded == null -> {
                    uiState = AlertedScoutUiState.ERROR
                    errorMessage = "Property not found"
                }
                loaded.isScouted -> {
                    property = loaded
                    uiState = AlertedScoutUiState.ALREADY_SCOUTED
                }
                else -> {
                    property = loaded
                    uiState = AlertedScoutUiState.READY
                }
            }
        } catch (e: Exception) {
            uiState = AlertedScoutUiState.ERROR
            errorMessage = e.message ?: "Failed to load property"
        }
    }

    suspend fun getCurrentLocationOrNull(): Pair<Double, Double>? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            return null
        }
        return runCatching { fusedLocationClient.lastLocation.await() }
            .getOrNull()
            ?.let { it.latitude to it.longitude }
    }

    fun navigateToProperty(target: Property) {
        val lat = target.latitude ?: return
        val lng = target.longitude ?: return
        val googleMapsIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$lat,$lng")
        ).apply {
            setPackage("com.google.android.apps.maps")
        }
        val fallbackIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        )
        val intent = if (googleMapsIntent.resolveActivity(context.packageManager) != null) {
            googleMapsIntent
        } else {
            fallbackIntent
        }
        context.startActivity(intent)
    }

    LaunchedEffect(apn) {
        loadProperty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerted Property") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (uiState) {
                AlertedScoutUiState.LOADING -> {
                    LoadingContent("Loading alerted property...")
                }

                AlertedScoutUiState.ERROR -> {
                    RetryCard(
                        message = errorMessage ?: "Failed to load property",
                        onRetry = {
                            scope.launch { loadProperty() }
                        },
                        onBack = onBack
                    )
                }

                AlertedScoutUiState.ALREADY_SCOUTED -> {
                    MessageCard(
                        title = "Already scouted",
                        body = "${property?.address ?: apn} already has a scouting result.",
                        buttonLabel = "Back",
                        onButtonClick = onBack
                    )
                }

                AlertedScoutUiState.COMPLETE -> {
                    MessageCard(
                        title = "Scout result saved",
                        body = "This property won't alert again once the backend reflects the new result.",
                        buttonLabel = "Done",
                        onButtonClick = onBack
                    )
                }

                AlertedScoutUiState.READY,
                AlertedScoutUiState.SUBMITTING -> {
                    val loadedProperty = property
                    if (loadedProperty == null) {
                        LoadingContent("Loading alerted property...")
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AlertedPropertyCard(
                                property = loadedProperty,
                                onNavigate = { navigateToProperty(loadedProperty) }
                            )

                            key(loadedProperty.apn) {
                                ScoutResultForm(
                                    property = loadedProperty,
                                    submitButtonText = if (uiState == AlertedScoutUiState.SUBMITTING) {
                                        "Saving..."
                                    } else {
                                        "Save Scout Result"
                                    },
                                    enabled = uiState != AlertedScoutUiState.SUBMITTING,
                                    onSubmit = { followUp, flyered, notes ->
                                        scope.launch {
                                            uiState = AlertedScoutUiState.SUBMITTING
                                            errorMessage = null
                                            try {
                                                val location = getCurrentLocationOrNull()
                                                scoutRepository.submitScoutResult(
                                                    apn = loadedProperty.apn,
                                                    followUp = followUp,
                                                    flyered = flyered,
                                                    notes = notes.takeIf { it.isNotBlank() },
                                                    latitude = location?.first,
                                                    longitude = location?.second
                                                )
                                                uiState = AlertedScoutUiState.COMPLETE
                                            } catch (e: Exception) {
                                                uiState = AlertedScoutUiState.READY
                                                errorMessage = e.message ?: "Failed to save scout result"
                                            }
                                        }
                                    }
                                )
                            }

                            errorMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertedPropertyCard(
    property: Property,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = property.address ?: property.apn,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "APN: ${property.apn}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            property.city?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (property.latitude != null && property.longitude != null) {
                Button(
                    onClick = onNavigate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate")
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("No map coordinates")
                }
            }
        }
    }
}

@Composable
private fun RetryCard(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String,
    buttonLabel: String,
    onButtonClick: () -> Unit
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = body)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onButtonClick) {
                Text(buttonLabel)
            }
        }
    }
}

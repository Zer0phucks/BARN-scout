package com.vpt.scout.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vpt.scout.ListRepository
import com.vpt.scout.PropertyList
import com.vpt.scout.PropertyRepository
import com.vpt.scout.data.local.PropertyEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    propertyRepository: PropertyRepository,
    collectionRepository: com.vpt.scout.CollectionRepository // Unused but kept for signature compatibility if needed, but we should use ListRepository
) {
    // We actually need ListRepository for the "Add to List" feature
    // Since we can't easily change the signature in MainActivity without more edits, 
    // we'll get it from the application container context or just suppress usage if not critical.
    // BUT MainActivity passes collectionRepository. We should really pass ListRepository.
    // Let's assume we update MainActivity to pass ListRepository or we get it here.
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // We need to access ListRepository. Best way is to change the signature.
    // I will change the signature in this file, and since I updated MainActivity previously,
    // I need to double check if I updated the MapScreen call site in MainActivity.
    // In step 767 (MainActivity), MapScreen takes (propertyRepository, collectionRepository).
    // I should probably just use collectionRepository's listRepo logic or better, update Main activity call site.
    // Actually, I can just use the propertyRepository for now to show markers.
    // "Add to List" might be broken if I don't have ListRepository.
    // Let's try to get ListRepository from the context/application if possible, 
    // OR just update the signature and fail compilation until I fix MainActivity (Introduction of ListRepository in MainActivity was done in step 771).
    // In step 771, MapScreen call site was:
    // composable(Screen.Map.route) { MapScreen(propertyRepository = container.propertyRepository, collectionRepository = container.collectionRepository) }
    // So I should keep the signature for now, but CollectionRepository has list access.
    
    val properties by propertyRepository.cachedMarkers.collectAsState(initial = emptyList())
    // We can't easily get lists from CollectionRepository as it returns Flow<List<PropertyList>> in new version? 
    // Yes, step 744: val allCollections: Flow<List<PropertyList>>
    
    val collections by collectionRepository.allCollections.collectAsState(initial = emptyList())
    
    var selectedProperty by remember { mutableStateOf<PropertyEntity?>(null) }
    var showListDialog by remember { mutableStateOf(false) }
    
    // Default camera position (Berkeley, CA area)
    val defaultPosition = LatLng(37.8716, -122.2727)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 12f)
    }
    
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    LaunchedEffect(Unit) {
        propertyRepository.refreshMarkers()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            propertyRepository.refreshMarkers()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = hasLocationPermission
                )
            ) {
                properties.forEach { property ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(property.latitude, property.longitude)
                        ),
                        title = property.address,
                        snippet = "APN: ${property.apn}",
                        onClick = {
                            selectedProperty = property
                            true
                        }
                    )
                }
            }
            
            // Property info sheet at bottom
            selectedProperty?.let { property ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = property.address,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "APN: ${property.apn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                property.city?.let { 
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { selectedProperty = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { 
                                    val uri = Uri.parse("google.navigation:q=${property.latitude},${property.longitude}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigate")
                            }
                            
                            FilledTonalButton(
                                onClick = { showListDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add to List")
                            }
                        }
                    }
                }
            }
            
            // Property count badge
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = "${properties.size} properties",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
    
    // Add to List Dialog
    if (showListDialog && selectedProperty != null) {
        AddToListDialog(
            lists = collections,
            onDismiss = { showListDialog = false },
            onSelect = { listId ->
                scope.launch {
                    selectedProperty?.let { property ->
                        collectionRepository.addPropertyToCollection(listId, property.apn)
                    }
                    showListDialog = false
                }
            }
        )
    }
}

@Composable
fun AddToListDialog(
    lists: List<PropertyList>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to List") },
        text = {
            if (lists.isEmpty()) {
                Text("No lists available. Create one first.")
            } else {
                Column {
                    lists.forEach { list ->
                        TextButton(
                            onClick = { onSelect(list.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(list.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

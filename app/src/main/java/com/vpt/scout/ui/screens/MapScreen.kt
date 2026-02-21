package com.vpt.scout.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vpt.scout.ListRepository
import com.vpt.scout.PropertyList
import com.vpt.scout.PropertyRepository
import com.vpt.scout.data.local.PropertyEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    propertyRepository: PropertyRepository,
    collectionRepository: com.vpt.scout.CollectionRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val properties by propertyRepository.cachedMarkers.collectAsState(initial = emptyList())
    val collections by collectionRepository.allCollections.collectAsState(initial = emptyList())
    
    var selectedProperty by remember { mutableStateOf<PropertyEntity?>(null) }
    var showListDialog by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoadingLocation by remember { mutableStateOf(true) }
    
    // FusedLocationProviderClient for getting user location
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Default camera position (Berkeley, CA area) - used as fallback
    val defaultPosition = LatLng(37.8716, -122.2727)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
    }
    
    // Calculate distance between two points in miles
    fun calculateDistanceMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMiles = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMiles * c
    }
    
    // Sort properties by distance from user
    val sortedProperties = remember(properties, userLocation) {
        userLocation?.let { loc ->
            properties.map { property ->
                val distance = calculateDistanceMiles(loc.latitude, loc.longitude, property.latitude, property.longitude)
                property to distance
            }.sortedBy { it.second }
        } ?: properties.map { it to 0.0 }
    }
    
    // Get user's current location
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng
                    // Animate camera to user location
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                        durationMs = 1000
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoadingLocation = false
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
                // Display markers with color coding based on distance
                sortedProperties.forEachIndexed { index, (property, distance) ->
                    // Color gradient: green for closest, yellow for medium, red for far
                    val markerColor = when {
                        index < 5 -> BitmapDescriptorFactory.HUE_GREEN  // Closest 5 properties
                        distance < 0.5 -> BitmapDescriptorFactory.HUE_GREEN  // Within 0.5 miles
                        distance < 1.0 -> BitmapDescriptorFactory.HUE_YELLOW  // Within 1 mile
                        distance < 2.0 -> BitmapDescriptorFactory.HUE_ORANGE  // Within 2 miles
                        else -> BitmapDescriptorFactory.HUE_RED  // More than 2 miles
                    }
                    
                    val distanceText = if (distance < 0.1) {
                        "${(distance * 5280).toInt()} ft"  // Show feet for very close properties
                    } else {
                        "%.2f mi".format(distance)
                    }
                    
                    Marker(
                        state = MarkerState(
                            position = LatLng(property.latitude, property.longitude)
                        ),
                        title = property.address,
                        snippet = "$distanceText • APN: ${property.apn}",
                        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                        zIndex = if (index < 5) 1f else 0f,  // Closest properties on top
                        onClick = {
                            selectedProperty = property
                            true
                        }
                    )
                }
            }
            
            // Property info sheet at bottom
            selectedProperty?.let { property ->
                // Calculate distance for selected property
                val selectedDistance = userLocation?.let { loc ->
                    calculateDistanceMiles(loc.latitude, loc.longitude, property.latitude, property.longitude)
                }
                val distanceText = selectedDistance?.let { dist ->
                    if (dist < 0.1) "${(dist * 5280).toInt()} ft away" else "%.2f mi away".format(dist)
                } ?: ""
                
                // Find the rank of this property
                val rank = sortedProperties.indexOfFirst { it.first.apn == property.apn } + 1
                
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
                                // Distance badge
                                if (distanceText.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.NearMe,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = distanceText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (rank > 0 && rank <= 5) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text("#$rank closest")
                                            }
                                        }
                                    }
                                }
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
            
            // Info cards at top
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Property count and nearest info
                Card {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${properties.size} properties",
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (sortedProperties.isNotEmpty() && userLocation != null) {
                            val closest = sortedProperties.first()
                            val nearestDist = if (closest.second < 0.1) {
                                "${(closest.second * 5280).toInt()} ft"
                            } else {
                                "%.2f mi".format(closest.second)
                            }
                            Text(
                                text = "Nearest: $nearestDist",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Center on me button
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val location = fusedLocationClient.lastLocation.await()
                                    if (location != null) {
                                        val latLng = LatLng(location.latitude, location.longitude)
                                        userLocation = latLng
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                                            durationMs = 500
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Center on my location",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Loading indicator
            if (isLoadingLocation) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Getting your location...")
                    }
                }
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

package com.vpt.scout.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import com.vpt.scout.ScoutRepository
import com.vpt.scout.ScoutStats
import com.vpt.scout.proximity.ProximityAlertPreferences
import com.vpt.scout.proximity.ProximityAlertSettings
import com.vpt.scout.proximity.ProximityMonitorService
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

internal enum class StatsContentState {
    LOADING,
    ERROR,
    READY,
    EMPTY
}

internal fun resolveStatsContentState(
    isLoading: Boolean,
    error: String?,
    stats: ScoutStats?
): StatsContentState {
    return when {
        isLoading -> StatsContentState.LOADING
        error != null -> StatsContentState.ERROR
        stats != null -> StatsContentState.READY
        else -> StatsContentState.EMPTY
    }
}

internal fun statsLoadErrorMessage(throwable: Throwable): String {
    return throwable.message?.takeIf { it.isNotBlank() } ?: "Failed to load scout stats"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    scoutRepository: ScoutRepository,
    proximityAlertPreferences: ProximityAlertPreferences,
    requestProximityPermissions: ((Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<ScoutStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val proximitySettings by proximityAlertPreferences.settings.collectAsState(
        initial = ProximityAlertSettings()
    )
    var proximityError by remember { mutableStateOf<String?>(null) }

    fun hasEssentialProximityPermissions(): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return locationGranted && notificationsGranted
    }

    fun updateMonitoring(enabled: Boolean) {
        if (enabled) {
            requestProximityPermissions { granted ->
                scope.launch {
                    if (granted || hasEssentialProximityPermissions()) {
                        proximityAlertPreferences.setEnabled(true)
                        proximityError = null
                        ContextCompat.startForegroundService(
                            context,
                            ProximityMonitorService.startIntent(context)
                        )
                    } else {
                        proximityAlertPreferences.setEnabled(false)
                        proximityError = "Location and notification permissions are required."
                    }
                }
            }
        } else {
            scope.launch {
                proximityAlertPreferences.setEnabled(false)
                proximityError = null
                context.stopService(ProximityMonitorService.stopIntent(context))
            }
        }
    }
    
    // Load stats
    LaunchedEffect(Unit) {
        try {
            stats = scoutRepository.getStats()
        } catch (e: Exception) {
            error = statsLoadErrorMessage(e)
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scout Stats") },
                actions = {
                    IconButton(onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                stats = scoutRepository.getStats()
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
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
            when (resolveStatsContentState(isLoading, error, stats)) {
                StatsContentState.LOADING -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                StatsContentState.ERROR -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            isLoading = true
                            error = null
                            scope.launch {
                                try {
                                    stats = scoutRepository.getStats()
                                } catch (e: Exception) {
                                    error = statsLoadErrorMessage(e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                StatsContentState.READY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Big number card
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
                                Text(
                                    "${stats!!.uniqueProperties}",
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Properties Scouted",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        
                        // Stats grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Total Visits",
                                value = stats!!.totalVisits.toString(),
                                icon = Icons.Default.DirectionsWalk,
                                color = Color(0xFF2196F3)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Follow-ups",
                                value = stats!!.followUps.toString(),
                                icon = Icons.Default.Flag,
                                color = Color(0xFFF44336)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Flyered",
                                value = stats!!.flyered.toString(),
                                icon = Icons.Default.Description,
                                color = Color(0xFF4CAF50)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Conversion",
                                value = if (stats!!.uniqueProperties > 0) {
                                    "${"%.0f".format(stats!!.followUps.toFloat() / stats!!.uniqueProperties * 100)}%"
                                } else "0%",
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFF9C27B0)
                            )
                        }

                        ProximityAlertsCard(
                            settings = proximitySettings,
                            status = when {
                                proximitySettings.enabled && hasEssentialProximityPermissions() ->
                                    "Monitoring is active. A foreground notification will stay visible."
                                proximitySettings.enabled ->
                                    "Monitoring wants permissions before it can stay active."
                                else ->
                                    "Off until you enable it."
                            },
                            error = proximityError,
                            onEnabledChange = { updateMonitoring(it) },
                            onThresholdSelected = { feet ->
                                scope.launch {
                                    proximityAlertPreferences.setThresholdFeet(feet)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                StatsContentState.EMPTY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Scout stats unavailable",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "The stats feed returned no data, but proximity alerts are still available below."
                                )
                            }
                        }

                        ProximityAlertsCard(
                            settings = proximitySettings,
                            status = when {
                                proximitySettings.enabled && hasEssentialProximityPermissions() ->
                                    "Monitoring is active. A foreground notification will stay visible."
                                proximitySettings.enabled ->
                                    "Monitoring wants permissions before it can stay active."
                                else ->
                                    "Off until you enable it."
                            },
                            error = proximityError,
                            onEnabledChange = { updateMonitoring(it) },
                            onThresholdSelected = { feet ->
                                scope.launch {
                                    proximityAlertPreferences.setThresholdFeet(feet)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProximityAlertsCard(
    settings: ProximityAlertSettings,
    status: String,
    error: String?,
    onEnabledChange: (Boolean) -> Unit,
    onThresholdSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Proximity Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = settings.thresholdFeet == 500,
                    onClick = { onThresholdSelected(500) },
                    label = { Text("500 ft") }
                )
                FilterChip(
                    selected = settings.thresholdFeet == 1000,
                    onClick = { onThresholdSelected(1000) },
                    label = { Text("1000 ft") }
                )
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.vpt.scout.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.vpt.scout.proximity.ProximityAlertPreferences
import com.vpt.scout.proximity.ProximityAlertSettings
import com.vpt.scout.proximity.ProximityMonitorService
import kotlinx.coroutines.launch

@Composable
fun ProximityAlertsSection(
    proximityAlertPreferences: ProximityAlertPreferences,
    requestProximityPermissions: ((Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by proximityAlertPreferences.settings.collectAsState(
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
                        runCatching {
                            ContextCompat.startForegroundService(
                                context,
                                ProximityMonitorService.startIntent(context)
                            )
                        }.onFailure {
                            proximityAlertPreferences.setEnabled(false)
                            proximityError = it.message ?: "Could not start proximity monitoring."
                        }
                    } else {
                        proximityAlertPreferences.setEnabled(false)
                        proximityError = "Foreground location and notifications are required."
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

    LaunchedEffect(settings.enabled) {
        if (settings.enabled && !hasEssentialProximityPermissions()) {
            proximityError = "Foreground location and notifications are required."
        }
    }

    ProximityAlertsCard(
        modifier = modifier,
        settings = settings,
        status = when {
            settings.enabled && hasEssentialProximityPermissions() ->
                "Monitoring is active. A foreground notification will stay visible."
            settings.enabled ->
                "Monitoring is waiting on required permissions."
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProximityAlertsCard(
    settings: ProximityAlertSettings,
    status: String,
    error: String?,
    onEnabledChange: (Boolean) -> Unit,
    onThresholdSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
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

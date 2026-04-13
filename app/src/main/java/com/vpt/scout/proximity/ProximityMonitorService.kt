package com.vpt.scout.proximity

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vpt.scout.Property
import com.vpt.scout.ScoutApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProximityMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: ProximityNotificationManager
    private lateinit var preferences: ProximityAlertPreferences
    private lateinit var repository: ProximityAlertRepository
    private lateinit var coordinator: ProximityAlertCoordinator
    private var locationCallback: LocationCallback? = null
    private var evaluationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as ScoutApplication).container
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = container.proximityNotificationManager
        preferences = container.proximityAlertPreferences
        repository = container.proximityAlertRepository
        coordinator = container.proximityAlertCoordinator
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            ProximityNotificationManager.NOTIFICATION_ID_MONITORING,
            notificationManager.buildMonitoringNotification()
        )
        startMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        if (!hasLocationPermission()) {
            notificationManager.showMonitoringNotification("Location permission required")
            stopSelf()
            return
        }

        if (locationCallback != null) {
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .setMinUpdateDistanceMeters(50f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val latest = result.lastLocation ?: return
                evaluateNearestProperty(latest)
            }
        }
        locationCallback = callback

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )

        serviceScope.launch {
            runCatching { fusedLocationClient.awaitLastLocationOrNull() }
                .getOrNull()
                ?.let { evaluateNearestProperty(it) }
                ?: notificationManager.showMonitoringNotification("Waiting for location")
        }
    }

    private fun stopMonitoring() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    private fun evaluateNearestProperty(location: Location) {
        evaluationJob?.cancel()
        evaluationJob = serviceScope.launch(Dispatchers.IO) {
            val settings = preferences.settings.first()
            if (!settings.enabled) {
                stopSelf()
                return@launch
            }

            val nearest = repository.getNearestUnscoutedProperty(
                latitude = location.latitude,
                longitude = location.longitude
            )

            if (nearest == null) {
                preferences.updateSuppression(
                    AlertSuppressionState(
                        lastAlertedApn = null,
                        lastInsideThreshold = false
                    )
                )
                notificationManager.showMonitoringNotification("No nearby unscouted properties")
                return@launch
            }

            val candidate = nearest.toAlertCandidate(location) ?: run {
                notificationManager.showMonitoringNotification("Nearest property missing map coordinates")
                return@launch
            }

            val suppression = preferences.suppressionState.first()
            val result = coordinator.evaluate(
                nearest = candidate,
                settings = settings,
                suppression = suppression
            )
            preferences.updateSuppression(result.nextSuppression)

            val status = buildStatusText(nearest, candidate.distanceFeet)
            notificationManager.showMonitoringNotification(status)
            if (result.shouldNotify) {
                notificationManager.showPropertyAlert(nearest, candidate.distanceFeet)
            }
        }
    }

    private fun buildStatusText(property: Property, distanceFeet: Float): String {
        val title = property.address ?: property.apn
        return "Closest: $title (${distanceFeet.toInt()} ft)"
    }

    private fun Property.toAlertCandidate(currentLocation: Location): AlertCandidate? {
        val propertyLat = latitude ?: return null
        val propertyLng = longitude ?: return null
        val results = FloatArray(1)
        Location.distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            propertyLat,
            propertyLng,
            results
        )
        val distanceFeet = results[0] * METERS_TO_FEET
        return AlertCandidate(
            apn = apn,
            distanceFeet = distanceFeet,
            isScouted = isScouted
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val METERS_TO_FEET = 3.28084f
        private const val ACTION_START = "com.vpt.scout.proximity.START"
        private const val ACTION_STOP = "com.vpt.scout.proximity.STOP"

        fun startIntent(context: Context): Intent {
            return Intent(context, ProximityMonitorService::class.java).apply {
                action = ACTION_START
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, ProximityMonitorService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}

private suspend fun FusedLocationProviderClient.awaitLastLocationOrNull(): Location? {
    return try {
        com.google.android.gms.tasks.Tasks.await(lastLocation)
    } catch (_: Exception) {
        null
    }
}

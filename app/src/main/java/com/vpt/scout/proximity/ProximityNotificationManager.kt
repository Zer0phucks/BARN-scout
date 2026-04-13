package com.vpt.scout.proximity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vpt.scout.MainActivity
import com.vpt.scout.Property
import kotlin.math.roundToInt

class ProximityNotificationManager(
    private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    fun buildMonitoringNotification(status: String = "Watching for nearby unscouted properties"): Notification {
        return NotificationCompat.Builder(context, CHANNEL_MONITORING)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Proximity alerts active")
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(buildAppPendingIntent())
            .build()
    }

    fun showMonitoringNotification(status: String = "Watching for nearby unscouted properties") {
        notificationManager.notify(
            NOTIFICATION_ID_MONITORING,
            buildMonitoringNotification(status)
        )
    }

    fun buildAlertPendingIntent(apn: String): PendingIntent {
        val route = buildAlertRoute(apn)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildAlertRoute(apn: String): String = "alerted-scout/$apn"

    fun showPropertyAlert(property: Property, distanceFeet: Float? = null) {
        val title = property.address ?: property.apn
        val distanceText = distanceFeet?.let { "${it.roundToInt()} ft away" }
        val content = listOfNotNull(distanceText, property.city).joinToString(" • ")
            .ifBlank { "Tap to open scout session" }

        notificationManager.notify(
            property.apn.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$title\n${property.apn}${if (content.isNotBlank()) "\n$content" else ""}")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(buildAlertPendingIntent(property.apn))
                .build()
        )
    }

    private fun buildAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val monitoringChannel = NotificationChannel(
            CHANNEL_MONITORING,
            "Proximity monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when background proximity monitoring is running."
        }
        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Nearby property alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when you are close to an unscouted property."
        }
        manager.createNotificationChannel(monitoringChannel)
        manager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val EXTRA_ROUTE = "route"
        const val CHANNEL_MONITORING = "proximity_monitoring"
        const val CHANNEL_ALERTS = "proximity_alerts"
        const val NOTIFICATION_ID_MONITORING = 41001
        private const val REQUEST_CODE_APP = 41002
    }
}

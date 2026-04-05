package com.vpt.scout.proximity

import android.app.PendingIntent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProximityNotificationManagerTest {

    @Test
    fun `alert notification opens alerted scout route with apn`() {
        val manager = ProximityNotificationManager(ApplicationProvider.getApplicationContext())

        val pendingIntent: PendingIntent = manager.buildAlertPendingIntent(apn = "001-100-100")
        val intent = shadowOf(pendingIntent).savedIntent

        assertEquals(
            "alerted-scout/001-100-100",
            intent.getStringExtra(ProximityNotificationManager.EXTRA_ROUTE)
        )
    }
}

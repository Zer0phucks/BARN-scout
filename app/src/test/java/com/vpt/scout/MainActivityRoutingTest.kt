package com.vpt.scout

import android.Manifest
import android.content.Intent
import android.os.Build
import com.vpt.scout.proximity.ProximityNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScoutApplication::class)
class MainActivityRoutingTest {

    @Test
    fun `extractRequestedRoute returns proximity route extra`() {
        val intent = Intent().apply {
            putExtra(
                ProximityNotificationManager.EXTRA_ROUTE,
                "alerted-scout/001-100-100"
            )
        }

        assertEquals("alerted-scout/001-100-100", extractRequestedRoute(intent))
    }

    @Test
    fun `launching main activity does not immediately start permission flow`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun `initial proximity permission request excludes background location`() {
        val permissions = buildInitialProximityPermissionsRequest(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

        assertTrue(Manifest.permission.ACCESS_FINE_LOCATION in permissions)
        assertTrue(Manifest.permission.ACCESS_COARSE_LOCATION in permissions)
        assertTrue(Manifest.permission.POST_NOTIFICATIONS in permissions)
        assertFalse(Manifest.permission.ACCESS_BACKGROUND_LOCATION in permissions)
    }

    @Test
    fun `initial proximity permission result only requires foreground location and notifications`() {
        val granted = areInitialProximityPermissionsGranted(
            permissions = mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to false,
                Manifest.permission.POST_NOTIFICATIONS to true,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION to false
            ),
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertTrue(granted)
    }
}

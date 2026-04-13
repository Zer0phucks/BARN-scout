package com.vpt.scout

import android.content.Intent
import com.vpt.scout.proximity.ProximityNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
}

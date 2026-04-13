package com.vpt.scout.proximity

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProximityAlertPreferencesTest {

    @Test
    fun `selected threshold persists as 1000 feet`() = runBlocking {
        val preferences = ProximityAlertPreferences(
            context = ApplicationProvider.getApplicationContext(),
            fileName = "proximity-test-${System.nanoTime()}.preferences_pb"
        )

        preferences.setThresholdFeet(1000)

        assertEquals(1000, preferences.settings.first().thresholdFeet)
    }
}

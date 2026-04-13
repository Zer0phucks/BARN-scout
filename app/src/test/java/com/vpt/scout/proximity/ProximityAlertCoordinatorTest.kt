package com.vpt.scout.proximity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityAlertCoordinatorTest {

    private val coordinator = ProximityAlertCoordinator()

    @Test
    fun `returns an alert when nearest property enters threshold and was not previously alerted`() {
        val result = coordinator.evaluate(
            nearest = AlertCandidate(apn = "001", distanceFeet = 420f, isScouted = false),
            settings = ProximityAlertSettings(enabled = true, thresholdFeet = 500),
            suppression = AlertSuppressionState(lastAlertedApn = null, lastInsideThreshold = false)
        )

        assertEquals("001", result.alertApn)
        assertTrue(result.shouldNotify)
    }

    @Test
    fun `does not notify again when closest property was already alerted and remains inside threshold`() {
        val result = coordinator.evaluate(
            nearest = AlertCandidate(apn = "001", distanceFeet = 410f, isScouted = false),
            settings = ProximityAlertSettings(enabled = true, thresholdFeet = 500),
            suppression = AlertSuppressionState(lastAlertedApn = "001", lastInsideThreshold = true)
        )

        assertEquals(null, result.alertApn)
        assertTrue(!result.shouldNotify)
    }
}

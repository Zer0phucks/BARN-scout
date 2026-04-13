package com.vpt.scout.ui.screens

import com.vpt.scout.ScoutStats
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsScreenStateTest {

    @Test
    fun `resolveStatsContentState returns empty when stats are missing without an error`() {
        val state = resolveStatsContentState(
            isLoading = false,
            error = null,
            stats = null
        )

        assertEquals(StatsContentState.EMPTY, state)
    }

    @Test
    fun `statsLoadErrorMessage falls back when exception message is null`() {
        val error = IllegalStateException()

        assertEquals("Failed to load scout stats", statsLoadErrorMessage(error))
    }

    @Test
    fun `resolveStatsContentState returns ready when stats are present`() {
        val state = resolveStatsContentState(
            isLoading = false,
            error = null,
            stats = ScoutStats(totalVisits = 3, followUps = 1, flyered = 2, uniqueProperties = 2)
        )

        assertEquals(StatsContentState.READY, state)
    }
}

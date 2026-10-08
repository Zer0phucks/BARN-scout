package com.vpt.scout.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectionsTest {

    @Test
    fun `uses coordinates when both are present`() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=37.88,-122.24&travelmode=driving",
            directionsUrl(37.88, -122.24, "1002 GRIZZLY PEAK BLVD, BERKELEY")
        )
    }

    @Test
    fun `falls back to the encoded address without coordinates`() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=879+REGAL+RD%2C+BERKELEY&travelmode=driving",
            directionsUrl(null, null, " 879 REGAL RD, BERKELEY ")
        )
    }

    @Test
    fun `ignores a lone coordinate`() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=DRURY+RD%2C+OAKLAND&travelmode=driving",
            directionsUrl(37.8, null, "DRURY RD, OAKLAND")
        )
    }

    @Test
    fun `returns null with no location at all`() {
        assertNull(directionsUrl(null, null, "  "))
    }
}

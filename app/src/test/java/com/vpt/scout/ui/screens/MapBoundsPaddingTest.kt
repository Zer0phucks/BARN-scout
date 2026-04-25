package com.vpt.scout.ui.screens

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.junit.Assert.assertTrue
import org.junit.Test

class MapBoundsPaddingTest {

    @Test
    fun `withPaddingFraction contains original corners`() {
        val original = LatLngBounds(
            LatLng(37.0, -122.1),
            LatLng(37.02, -122.08)
        )
        val padded = original.withPaddingFraction(0.25)

        assertTrue(padded.contains(original.southwest))
        assertTrue(padded.contains(original.northeast))
    }
}

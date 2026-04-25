package com.vpt.scout

import com.google.android.gms.maps.model.LatLng
import com.vpt.scout.ui.screens.initializeMapScreen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapScreenInitializationTest {

    @Test
    fun `initializeMapScreen completes even if location lookup times out`() = runBlocking {
        var loading = true
        var centeredLocation: LatLng? = null

        initializeMapScreen(
            hasLocationPermission = true,
            fetchLastLocation = { suspendCancellableCoroutine { } },
            onLocationAvailable = { centeredLocation = it },
            onLoadingLocationChanged = { loading = it },
            locationTimeoutMillis = 1
        )

        assertFalse(loading)
        assertEquals(null, centeredLocation)
    }
}

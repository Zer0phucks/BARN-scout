package com.vpt.scout.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.vpt.scout.Property
import java.net.URLEncoder

/**
 * Google Maps universal directions link. It opens the Maps app's directions
 * screen when Maps is installed and the browser otherwise, unlike
 * `google.navigation:` which needs the Maps app and skips the route preview.
 *
 * Coordinates are preferred; the address is the fallback for properties the
 * geocoder could not place. Returns null when neither is available.
 */
internal fun directionsUrl(latitude: Double?, longitude: Double?, address: String?): String? {
    val destination = when {
        latitude != null && longitude != null -> "$latitude,$longitude"
        !address.isNullOrBlank() -> URLEncoder.encode(address.trim(), Charsets.UTF_8.name())
        else -> return null
    }
    return "https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=driving"
}

internal fun openDirections(context: Context, latitude: Double?, longitude: Double?, address: String?) {
    val url = directionsUrl(latitude, longitude, address)
    if (url == null) {
        Toast.makeText(context, "No location for this property", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open directions", Toast.LENGTH_SHORT).show()
    }
}

internal fun openDirections(context: Context, property: Property) =
    openDirections(context, property.latitude, property.longitude, property.address)

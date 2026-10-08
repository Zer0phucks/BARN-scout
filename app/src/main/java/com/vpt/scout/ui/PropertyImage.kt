package com.vpt.scout.ui

/**
 * Image to show for a property, or null for the placeholder.
 *
 * Property images are the scanner's captures from scan/streetview_images,
 * uploaded to the public `streetview-images` Supabase bucket by
 * scan/scripts/upload_streetview_images.py, which stores the public URL in
 * streetview_image_path. The column can also hold a scanner-local file path
 * when an upload failed; the phone cannot load those, so only http(s) URLs
 * are used.
 */
internal fun propertyImageUrl(storedPath: String?): String? {
    val stored = storedPath?.trim() ?: return null
    return stored.takeIf { it.startsWith("https://") || it.startsWith("http://") }
}

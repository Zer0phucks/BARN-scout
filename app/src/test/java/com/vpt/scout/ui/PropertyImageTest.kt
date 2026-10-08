package com.vpt.scout.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PropertyImageTest {

    @Test
    fun `uses the stored public url`() {
        val stored = "https://ndjq.supabase.co/storage/v1/object/public/streetview-images/63-2969-19.jpg"
        assertEquals(stored, propertyImageUrl(" $stored "))
    }

    @Test
    fun `ignores scanner-local paths`() {
        assertNull(propertyImageUrl("streetview_images/63-2969-19.jpg"))
        assertNull(propertyImageUrl("/home/noob/barn/scan/streetview_images/63-2969-19.jpg"))
    }

    @Test
    fun `returns null when nothing is stored`() {
        assertNull(propertyImageUrl(null))
        assertNull(propertyImageUrl(""))
    }
}

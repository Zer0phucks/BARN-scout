package com.vpt.scout.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CyberBackgroundChecksTest {
    @Test
    fun buildsAddressSearchWithNormalizedWhitespaceAndZipPlusFour() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/address/1008-jones-st/berkeley/94710",
            cyberBackgroundChecksUrl(" 1008  JONES ST ", " BERKELEY ", "94710-1234")
        )
    }

    @Test
    fun stripsCityAndZipFromFullAddress() {
        val expected = "https://www.cyberbackgroundchecks.com/address/1008-jones-st/berkeley/94710"
        assertEquals(expected, cyberBackgroundChecksUrl("1008 Jones St, Berkeley CA 94710", "Berkeley", null))
        assertEquals(expected, cyberBackgroundChecksUrl("1008 Jones St Berkeley CA 94710", "Berkeley", "94710"))
    }

    @Test
    fun encodesPathSeparatorsAndQueryCharacters() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/address/12-a%2Fb-%23-2/oakland/94601",
            cyberBackgroundChecksUrl("12 A/B # 2", "Oakland", "94601")
        )
    }

    @Test
    fun missingAddressPartsDoNotProduceMisleadingLinks() {
        assertNull(cyberBackgroundChecksUrl(null, "Berkeley", "94710"))
        assertNull(cyberBackgroundChecksUrl("Unknown", "Berkeley", "94710"))
        assertNull(cyberBackgroundChecksUrl("1008 Jones St", "", "94710"))
        assertNull(cyberBackgroundChecksUrl("1008 Jones St", "Berkeley", null))
    }
}

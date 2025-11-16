package com.app.miklink.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityTest {

    @Test
    fun `isTdrSupported returns true for a supported model`() {
        // Test with a model known to support TDR
        assertTrue(Compatibility.isTdrSupported("RB4011iGS+RM"))
    }

    @Test
    fun `isTdrSupported returns false for an unsupported model`() {
        // Test with a model known not to support TDR
        assertFalse(Compatibility.isTdrSupported("hAP lite"))
    }

    @Test
    fun `isTdrSupported returns false for null or empty board name`() {
        // Test edge cases with null and empty strings
        assertFalse(Compatibility.isTdrSupported(null))
        assertFalse(Compatibility.isTdrSupported(""))
        assertFalse(Compatibility.isTdrSupported(" "))
    }

    @Test
    fun `isTdrSupported is case-insensitive`() {
        // Test with a lowercase version of a supported model
        assertTrue(Compatibility.isTdrSupported("rb4011igs+"))
        assertTrue(Compatibility.isTdrSupported("ccr1009"))
    }

    @Test
    fun `isTdrSupported returns true for partial matches`() {
        // Test if a board name containing a supported model works
        assertTrue(Compatibility.isTdrSupported("My Router hEX S"))
        assertTrue(Compatibility.isTdrSupported("MikroTik CCR1016-12G"))
    }
}

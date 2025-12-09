package com.app.miklink.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkUtilsTest {

    // --- Status Tests ---

    @Test
    fun `normalizeLinkStatus handles connected states`() {
        assertEquals("Connesso", normalizeLinkStatus("link-ok"))
        assertEquals("Connesso", normalizeLinkStatus("ok"))
        assertEquals("Connesso", normalizeLinkStatus("up"))
        assertEquals("Connesso", normalizeLinkStatus("running"))
        assertEquals("Connesso", normalizeLinkStatus("LINK-OK")) // Case insensitive
    }

    @Test
    fun `normalizeLinkStatus handles disconnected states`() {
        assertEquals("Disconnesso", normalizeLinkStatus("no-link"))
        assertEquals("Disconnesso", normalizeLinkStatus("down"))
        assertEquals("Disconnesso", normalizeLinkStatus("NO-LINK"))
    }

    @Test
    fun `normalizeLinkStatus handles unknown state`() {
        assertEquals("Sconosciuto", normalizeLinkStatus("unknown"))
        assertEquals("Sconosciuto", normalizeLinkStatus("UNKNOWN"))
    }

    @Test
    fun `normalizeLinkStatus handles empty or null`() {
        assertEquals("N/A", normalizeLinkStatus(null))
        assertEquals("N/A", normalizeLinkStatus(""))
        assertEquals("N/A", normalizeLinkStatus("  "))
    }

    @Test
    fun `normalizeLinkStatus handles fallback to N_A`() {
        assertEquals("N/A", normalizeLinkStatus("testing"))
        assertEquals("N/A", normalizeLinkStatus("invalid-status"))
    }

    // --- Speed Tests ---

    @Test
    fun `normalizeLinkSpeed formats standard speeds`() {
        assertEquals("1 Gbps", normalizeLinkSpeed("1Gbps"))
        assertEquals("100 Mbps", normalizeLinkSpeed("100Mbps"))
        assertEquals("10 Mbps", normalizeLinkSpeed("10Mbps"))
        assertEquals("2.5 Gbps", normalizeLinkSpeed("2.5Gbps"))
    }

    @Test
    fun `normalizeLinkSpeed handles high speeds`() {
        assertEquals("10 Gbps", normalizeLinkSpeed("10Gbps"))
        assertEquals("25 Gbps", normalizeLinkSpeed("25Gbps"))
        assertEquals("40 Gbps", normalizeLinkSpeed("40Gbps"))
        assertEquals("100 Gbps", normalizeLinkSpeed("100Gbps"))
        assertEquals("400 Gbps", normalizeLinkSpeed("400Gbps"))
    }

    @Test
    fun `normalizeLinkSpeed handles empty or null`() {
        assertEquals("N/A", normalizeLinkSpeed(null))
        assertEquals("N/A", normalizeLinkSpeed(""))
        assertEquals("N/A", normalizeLinkSpeed("   "))
    }

    @Test
    fun `normalizeLinkSpeed ignores invalid formats`() {
        assertEquals("10M-baseT-half", normalizeLinkSpeed("10M-baseT-half"))
        assertEquals("invalid", normalizeLinkSpeed("invalid"))
    }
    
    @Test
    fun `normalizeLinkSpeed handles values with existing spaces`() {
        // Should trim and re-format correctly
        assertEquals("1 Gbps", normalizeLinkSpeed("1 Gbps"))
        assertEquals("100 Mbps", normalizeLinkSpeed("100 Mbps"))
    }
}

package com.app.miklink.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit Test Suite for RateParser
 *
 * Tests the parsing logic for network speed rate strings to Mbps (integer).
 * Covers valid formats, edge cases, invalid inputs, and null handling.
 */
class RateParserTest {

    // ============================================
    // TEST: Valid Formats - Gigabit Speeds
    // ============================================

    @Test
    fun `parseToMbps with 10G returns 10000 Mbps`() {
        assertEquals(10000, RateParser.parseToMbps("10G"))
    }

    @Test
    fun `parseToMbps with 10Gbps returns 10000 Mbps`() {
        assertEquals(10000, RateParser.parseToMbps("10Gbps"))
    }

    @Test
    fun `parseToMbps with 10GB returns 10000 Mbps`() {
        assertEquals(10000, RateParser.parseToMbps("10GB"))
    }

    @Test
    fun `parseToMbps with 1G returns 1000 Mbps`() {
        assertEquals(1000, RateParser.parseToMbps("1G"))
    }

    @Test
    fun `parseToMbps with 1Gbps returns 1000 Mbps`() {
        assertEquals(1000, RateParser.parseToMbps("1Gbps"))
    }

    @Test
    fun `parseToMbps with 1GB returns 1000 Mbps`() {
        assertEquals(1000, RateParser.parseToMbps("1GB"))
    }

    @Test
    fun `parseToMbps with 1_2Gbps returns 1200 Mbps`() {
        // Decimal Gbps values
        assertEquals(1200, RateParser.parseToMbps("1.2Gbps"))
    }

    @Test
    fun `parseToMbps with 2_5Gbps returns 2500 Mbps`() {
        assertEquals(2500, RateParser.parseToMbps("2.5Gbps"))
    }

    @Test
    fun `parseToMbps with 5G returns 5000 Mbps`() {
        assertEquals(5000, RateParser.parseToMbps("5G"))
    }

    @Test
    fun `parseToMbps with 10_5G returns 10500 Mbps`() {
        assertEquals(10500, RateParser.parseToMbps("10.5G"))
    }

    // ============================================
    // TEST: Valid Formats - Megabit Speeds
    // ============================================

    @Test
    fun `parseToMbps with 100M returns 100 Mbps`() {
        assertEquals(100, RateParser.parseToMbps("100M"))
    }

    @Test
    fun `parseToMbps with 100Mbps returns 100 Mbps`() {
        assertEquals(100, RateParser.parseToMbps("100Mbps"))
    }

    @Test
    fun `parseToMbps with 100MB returns 100 Mbps`() {
        assertEquals(100, RateParser.parseToMbps("100MB"))
    }

    @Test
    fun `parseToMbps with 10M returns 10 Mbps`() {
        assertEquals(10, RateParser.parseToMbps("10M"))
    }

    @Test
    fun `parseToMbps with 10Mbps returns 10 Mbps`() {
        assertEquals(10, RateParser.parseToMbps("10Mbps"))
    }

    @Test
    fun `parseToMbps with 50Mbps returns 50 Mbps`() {
        assertEquals(50, RateParser.parseToMbps("50Mbps"))
    }

    @Test
    fun `parseToMbps with 250M returns 250 Mbps`() {
        assertEquals(250, RateParser.parseToMbps("250M"))
    }

    @Test
    fun `parseToMbps with 500Mbps returns 500 Mbps`() {
        assertEquals(500, RateParser.parseToMbps("500Mbps"))
    }

    @Test
    fun `parseToMbps with 75_5Mbps returns 75 Mbps`() {
        // Decimal Mbps values (truncated to int)
        assertEquals(75, RateParser.parseToMbps("75.5Mbps"))
    }

    // ============================================
    // TEST: Valid Formats - Kilobit Speeds
    // ============================================

    @Test
    fun `parseToMbps with 50Kbps returns 0 Mbps`() {
        // 50 Kbps = 0.05 Mbps → truncated to 0
        assertEquals(0, RateParser.parseToMbps("50K"))
    }

    @Test
    fun `parseToMbps with 1000K returns 1 Mbps`() {
        // 1000 Kbps = 1 Mbps
        assertEquals(1, RateParser.parseToMbps("1000K"))
    }

    @Test
    fun `parseToMbps with 5000K returns 5 Mbps`() {
        // 5000 Kbps = 5 Mbps
        assertEquals(5, RateParser.parseToMbps("5000K"))
    }

    @Test
    fun `parseToMbps with 500_5K returns 0 Mbps`() {
        // 500.5 Kbps = 0.5005 Mbps → truncated to 0
        assertEquals(0, RateParser.parseToMbps("500.5K"))
    }

    // ============================================
    // TEST: Valid Formats - Plain Numbers
    // ============================================

    @Test
    fun `parseToMbps with plain number 100 returns 100 Mbps`() {
        // Assume plain numbers are already in Mbps
        assertEquals(100, RateParser.parseToMbps("100"))
    }

    @Test
    fun `parseToMbps with plain number 1000 returns 1000 Mbps`() {
        assertEquals(1000, RateParser.parseToMbps("1000"))
    }

    @Test
    fun `parseToMbps with plain number 50 returns 50 Mbps`() {
        assertEquals(50, RateParser.parseToMbps("50"))
    }

    // ============================================
    // TEST: Edge Cases - Case Insensitivity
    // ============================================

    @Test
    fun `parseToMbps with lowercase 100mbps returns 100 Mbps`() {
        assertEquals(100, RateParser.parseToMbps("100mbps"))
    }

    @Test
    fun `parseToMbps with mixed case 1Gbps returns 1000 Mbps`() {
        assertEquals(1000, RateParser.parseToMbps("1Gbps"))
    }

    @Test
    fun `parseToMbps with lowercase 1g returns 1000 Mbps`() {
        assertEquals(1000, RateParser.parseToMbps("1g"))
    }

    // ============================================
    // TEST: Edge Cases - Whitespace
    // ============================================

    @Test
    fun `parseToMbps with leading whitespace returns correct value`() {
        assertEquals(100, RateParser.parseToMbps("  100Mbps"))
    }

    @Test
    fun `parseToMbps with trailing whitespace returns correct value`() {
        assertEquals(100, RateParser.parseToMbps("100Mbps  "))
    }

    @Test
    fun `parseToMbps with whitespace in middle returns correct value`() {
        assertEquals(100, RateParser.parseToMbps("100 Mbps"))
    }

    @Test
    fun `parseToMbps with multiple spaces returns correct value`() {
        assertEquals(1000, RateParser.parseToMbps("  1  G  "))
    }

    // ============================================
    // TEST: Edge Cases - Zero and Small Values
    // ============================================

    @Test
    fun `parseToMbps with 0Mbps returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("0Mbps"))
    }

    @Test
    fun `parseToMbps with 0M returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("0M"))
    }

    @Test
    fun `parseToMbps with 0G returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("0G"))
    }

    @Test
    fun `parseToMbps with 0_5Mbps returns 0 Mbps`() {
        // 0.5 Mbps truncated to 0
        assertEquals(0, RateParser.parseToMbps("0.5Mbps"))
    }

    @Test
    fun `parseToMbps with 1Mbps returns 1 Mbps`() {
        assertEquals(1, RateParser.parseToMbps("1Mbps"))
    }

    // ============================================
    // TEST: Invalid Formats - Non-numeric
    // ============================================

    @Test
    fun `parseToMbps with unknown returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("unknown"))
    }

    @Test
    fun `parseToMbps with abc returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("abc"))
    }

    @Test
    fun `parseToMbps with random text returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("random text"))
    }

    @Test
    fun `parseToMbps with special characters returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("@#$%"))
    }

    @Test
    fun `parseToMbps with mixed alphanumeric invalid format returns extracted digits`() {
        // "100xyz" → extracts "100"
        assertEquals(100, RateParser.parseToMbps("100xyz"))
    }

    @Test
    fun `parseToMbps with mixed invalid abc100 returns 100`() {
        // "abc100" → extracts "100"
        assertEquals(100, RateParser.parseToMbps("abc100"))
    }

    @Test
    fun `parseToMbps with mixed invalid abc100def returns 100`() {
        // "abc100def" → extracts "100"
        assertEquals(100, RateParser.parseToMbps("abc100def"))
    }

    // ============================================
    // TEST: Invalid Formats - Empty and Null
    // ============================================

    @Test
    fun `parseToMbps with empty string returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps(""))
    }

    @Test
    fun `parseToMbps with null returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps(null))
    }

    @Test
    fun `parseToMbps with blank string returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("   "))
    }

    @Test
    fun `parseToMbps with newline returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("\n"))
    }

    @Test
    fun `parseToMbps with tab returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("\t"))
    }

    // ============================================
    // TEST: Invalid Formats - Unsupported Units
    // ============================================

    @Test
    fun `parseToMbps with Tbps returns extracted digits`() {
        // Terabit not supported - parser extracts "1" from "1Tbps"
        assertEquals(1, RateParser.parseToMbps("1Tbps"))
    }

    @Test
    fun `parseToMbps with bps (bits per second) returns extracted digits`() {
        // "100bps" → extracts "100"
        assertEquals(100, RateParser.parseToMbps("100bps"))
    }

    // ============================================
    // TEST: Edge Cases - Decimal Precision
    // ============================================

    @Test
    fun `parseToMbps with 1_99Gbps returns 1990 Mbps`() {
        assertEquals(1990, RateParser.parseToMbps("1.99Gbps"))
    }

    @Test
    fun `parseToMbps with 0_001Gbps returns 1 Mbps (was bug, now fixed)`() {
        // FIXED: "0.001Gbps" → "0.001GBPS" now correctly matches s.endsWith("GBPS") first
        // 0.001 * 1000 = 1 Mbps (correct)
        assertEquals(1, RateParser.parseToMbps("0.001Gbps"))
    }

    @Test
    fun `parseToMbps with 999_9Mbps returns 999 Mbps`() {
        assertEquals(999, RateParser.parseToMbps("999.9Mbps"))
    }

    // ============================================
    // TEST: formatReadable() - Output Formatting
    // ============================================

    @Test
    fun `formatReadable with 10000 Mbps returns 10Gbps`() {
        assertEquals("10Gbps", RateParser.formatReadable(10000))
    }

    @Test
    fun `formatReadable with 1000 Mbps returns 1Gbps`() {
        assertEquals("1Gbps", RateParser.formatReadable(1000))
    }

    @Test
    fun `formatReadable with 1500 Mbps returns 1Gbps`() {
        // 1500 / 1000 = 1 (integer division)
        assertEquals("1Gbps", RateParser.formatReadable(1500))
    }

    @Test
    fun `formatReadable with 100 Mbps returns 100Mbps`() {
        assertEquals("100Mbps", RateParser.formatReadable(100))
    }

    @Test
    fun `formatReadable with 50 Mbps returns 50Mbps`() {
        assertEquals("50Mbps", RateParser.formatReadable(50))
    }

    @Test
    fun `formatReadable with 1 Mbps returns 1Mbps`() {
        assertEquals("1Mbps", RateParser.formatReadable(1))
    }

    @Test
    fun `formatReadable with 0 Mbps returns dash`() {
        assertEquals("-", RateParser.formatReadable(0))
    }

    @Test
    fun `formatReadable with negative value returns dash`() {
        assertEquals("-", RateParser.formatReadable(-10))
    }

    // ============================================
    // TEST: Round-trip Consistency
    // ============================================

    @Test
    fun `parseToMbps and formatReadable roundtrip for 1Gbps`() {
        val parsed = RateParser.parseToMbps("1Gbps")
        val formatted = RateParser.formatReadable(parsed)
        assertEquals("1Gbps", formatted)
    }

    @Test
    fun `parseToMbps and formatReadable roundtrip for 100Mbps`() {
        val parsed = RateParser.parseToMbps("100Mbps")
        val formatted = RateParser.formatReadable(parsed)
        assertEquals("100Mbps", formatted)
    }

    @Test
    fun `parseToMbps and formatReadable roundtrip for 10G`() {
        val parsed = RateParser.parseToMbps("10G")
        val formatted = RateParser.formatReadable(parsed)
        assertEquals("10Gbps", formatted)
    }

    // ============================================
    // TEST: Real-world MikroTik API Values
    // ============================================

    @Test
    fun `parseToMbps with MikroTik format 1000Mbps returns 1000 Mbps`() {
        // Common MikroTik auto-negotiation output
        assertEquals(1000, RateParser.parseToMbps("1000Mbps"))
    }

    @Test
    fun `parseToMbps with MikroTik format 100Mbps-full returns 100 Mbps`() {
        // MikroTik may return "100Mbps-full" for full duplex
        // Parser extracts "100"
        assertEquals(100, RateParser.parseToMbps("100Mbps-full"))
    }

    @Test
    fun `parseToMbps with MikroTik format 10Mbps-half returns 10 Mbps`() {
        assertEquals(10, RateParser.parseToMbps("10Mbps-half"))
    }

    @Test
    fun `parseToMbps with MikroTik format link-down returns 0 Mbps`() {
        // "link-down" or similar status strings
        assertEquals(0, RateParser.parseToMbps("link-down"))
    }

    @Test
    fun `parseToMbps with MikroTik format no-link returns 0 Mbps`() {
        assertEquals(0, RateParser.parseToMbps("no-link"))
    }
}


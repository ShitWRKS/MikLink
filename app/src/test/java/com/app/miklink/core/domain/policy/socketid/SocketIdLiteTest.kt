/*
 * Purpose: Validate SocketIdLite formatting, parsing, and gap detection to keep UI/dashboard behavior aligned with ADR-0004.
 * Inputs: Prefix/separator/padding/suffix combinations, formatted socket names, and collections of existing numeric ids.
 * Outputs: Assertions guaranteeing deterministic format/parse output and firstMissingPositive gap selection.
 * Notes: Tests cover blank prefix/suffix cases and realistic names to avoid regressions in auto-increment.
 */
package com.app.miklink.core.domain.policy.socketid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocketIdLiteTest {

    @Test
    fun `format preserves separators with prefix and suffix`() {
        val formatted = SocketIdLite.format(prefix = "SW", separator = "-", numberPadding = 3, suffix = "A", idNumber = 1)
        assertEquals("SW-001-A", formatted)
    }

    @Test
    fun `format omits leading and trailing separators when prefix and suffix are blank`() {
        val formatted = SocketIdLite.format(prefix = "", separator = "-", numberPadding = 2, suffix = "", idNumber = 1)
        assertEquals("01", formatted)
    }

    @Test
    fun `format omits trailing separator when suffix is blank`() {
        val formatted = SocketIdLite.format(prefix = "SW", separator = "-", numberPadding = 3, suffix = "", idNumber = 1)
        assertEquals("SW-001", formatted)
    }

    @Test
    fun `format omits leading separator when prefix is blank but keeps suffix separator`() {
        val formatted = SocketIdLite.format(prefix = "", separator = "-", numberPadding = 2, suffix = "A", idNumber = 1)
        assertEquals("01-A", formatted)
    }

    @Test
    fun `parse extracts numeric id from formatted socket name`() {
        val parsed = SocketIdLite.parseIdNumber(socketName = "SW-001-A", prefix = "SW", separator = "-")
        assertEquals(1, parsed)
    }

    @Test
    fun `parse works with blank prefix and suffix using new format`() {
        val parsed = SocketIdLite.parseIdNumber(socketName = "01", prefix = "", separator = "-")
        assertEquals(1, parsed)
    }

    @Test
    fun `parse accepts legacy separators when prefix and suffix are blank`() {
        val parsed = SocketIdLite.parseIdNumber(socketName = "-01-", prefix = "", separator = "-")
        assertEquals(1, parsed)
    }

    @Test
    fun `parse handles suffix without leading separator when prefix is blank`() {
        val parsed = SocketIdLite.parseIdNumber(socketName = "01-A", prefix = "", separator = "-")
        assertEquals(1, parsed)
    }

    @Test
    fun `parse handles missing suffix separator when suffix blank`() {
        val parsed = SocketIdLite.parseIdNumber(socketName = "SW-001", prefix = "SW", separator = "-")
        assertEquals(1, parsed)
    }

    @Test
    fun `parse returns null for missing separators`() {
        assertNull(SocketIdLite.parseIdNumber(socketName = "SW001A", prefix = "SW", separator = "-"))
        assertNull(SocketIdLite.parseIdNumber(socketName = "SW-ABC-A", prefix = "SW", separator = "-"))
    }

    @Test
    fun `firstMissingPositive returns first hole or next sequential`() {
        assertEquals(3, SocketIdLite.firstMissingPositive(listOf(1, 2, 4)))
        assertEquals(1, SocketIdLite.firstMissingPositive(emptyList()))
        assertEquals(5, SocketIdLite.firstMissingPositive(listOf(1, 2, 3, 4)))
    }
}

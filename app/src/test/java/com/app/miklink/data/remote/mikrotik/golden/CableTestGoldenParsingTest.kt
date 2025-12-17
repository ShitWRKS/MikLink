/*
 * Purpose: Guard Moshi parsing of MikroTik cable test golden responses for link-ok and no-link scenarios.
 * Inputs: RouterOS 7.20.5 cable test fixtures and TestMoshiProvider configuration.
 * Outputs: Assertions on parsed status and cablePairs values.
 */
package com.app.miklink.data.remote.mikrotik.golden

import com.app.miklink.testsupport.FixtureLoader
import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test

class CableTestGoldenParsingTest {

    data class GoldenCableTest(
        val name: String?,
        val status: String?,
        @param:Json(name = "cable-pairs") val cablePairs: String?
    )

    @Test
    fun `parse cable test link ok no measurement`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/ethernet_cable_test_ether1_link_ok.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, GoldenCableTest::class.java)
        val adapter = moshi.adapter<List<GoldenCableTest>>(type)

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(1, parsed?.size)
        val item = parsed!!.first()
        assertEquals("link-ok", item.status)
        assertNull(item.cablePairs)
    }

    @Test
    fun `parse cable test no link with cable pairs`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/ethernet_cable_test_ether2_no_link_open.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, GoldenCableTest::class.java)
        val adapter = moshi.adapter<List<GoldenCableTest>>(type)

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(1, parsed?.size)
        val item = parsed!!.first()
        assertEquals("no-link", item.status)
        assertEquals("open:4,open:4,open:4,open:4", item.cablePairs)
    }
}

/*
 * Purpose: Ensure Moshi correctly parses ethernet monitor golden responses for link and duplex status.
 * Inputs: RouterOS 7.20.5 ethernet monitor fixtures and configured Moshi instance.
 * Outputs: Assertions on parsed link status, rate, and duplex flag.
 */
package com.app.miklink.data.remote.mikrotik.golden

import com.app.miklink.testsupport.FixtureLoader
import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test

class EthernetMonitorGoldenParsingTest {

    data class GoldenEthernetMonitor(
        val name: String?,
        val status: String?,
        val rate: String?,
        @param:Json(name = "full-duplex") val fullDuplex: String?
    )

    @Test
    fun `parse ethernet monitor link ok 1gbps`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/ethernet_monitor_ether1_link_ok_1gbps.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, GoldenEthernetMonitor::class.java)
        val adapter = moshi.adapter<List<GoldenEthernetMonitor>>(type)

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(1, parsed?.size)
        val item = parsed!!.first()
        assertEquals("link-ok", item.status)
        assertEquals("1Gbps", item.rate)
        assertEquals("true", item.fullDuplex)
    }

    @Test
    fun `parse ethernet monitor no link rate absent becomes null`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/ethernet_monitor_ether2_no_link.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, GoldenEthernetMonitor::class.java)
        val adapter = moshi.adapter<List<GoldenEthernetMonitor>>(type)

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(1, parsed?.size)
        val item = parsed!!.first()
        assertEquals("no-link", item.status)
        assertNull(item.rate)
    }
}

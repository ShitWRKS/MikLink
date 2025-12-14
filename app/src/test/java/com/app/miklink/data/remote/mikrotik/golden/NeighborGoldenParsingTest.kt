package com.app.miklink.data.remote.mikrotik.golden

import com.app.miklink.testsupport.FixtureLoader
import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test

class NeighborGoldenParsingTest {
    data class GoldenNeighbor(
        @Json(name = "mac-address") val macAddress: String?,
        @Json(name = "discovered-by") val discoveredBy: String?,
        @Json(name = "system-caps") val systemCaps: String?,
        @Json(name = "interface") val iface: String?,
        @Json(name = "interface-name") val interfaceName: String?
    )

    @Test
    fun `parse neighbor golden fixture`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/ip_neighbor_single.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, GoldenNeighbor::class.java)
        val adapter = moshi.adapter<List<GoldenNeighbor>>(type)

        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals(1, parsed?.size)
        val neighbor = parsed!!.first()
        assertEquals("2C:C8:1B:F0:A8:BA", neighbor.macAddress)
        assertEquals("cdp,lldp,mndp", neighbor.discoveredBy)
        assertEquals("bridge,router", neighbor.systemCaps)
        assertEquals("ether1", neighbor.iface)
        assertEquals("bridge_lan/ether2", neighbor.interfaceName)
    }
}

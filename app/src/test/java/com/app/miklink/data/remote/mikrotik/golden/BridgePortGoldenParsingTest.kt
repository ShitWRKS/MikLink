package com.app.miklink.data.remote.mikrotik.golden

import com.app.miklink.testsupport.FixtureLoader
import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test

class BridgePortGoldenParsingTest {
    data class GoldenBridgePort(
        val bridge: String?,
        val `interface`: String?,
        val pvid: String?
    )

    @Test
    fun `parse bridge ports assert bridge and pvid`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/bridge_port.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, GoldenBridgePort::class.java)
        val adapter = moshi.adapter<List<GoldenBridgePort>>(type)

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertTrue(parsed!!.any { it.bridge == "bridge1" && it.`interface` != null && it.pvid != null })
    }
}

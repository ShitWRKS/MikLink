/*
 * Purpose: Verify Moshi parsing for bridge host golden file ensuring MAC and interface are read correctly.
 * Inputs: RouterOS 7.20.5 bridge_host.json fixture via FixtureLoader and TestMoshiProvider.
 * Outputs: Assertions that parsed hosts contain expected MAC/interface combination.
 */
package com.app.miklink.data.remote.mikrotik.golden

import com.app.miklink.testsupport.FixtureLoader
import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test

class BridgeHostGoldenParsingTest {
    data class GoldenBridgeHost(
        @param:Json(name = "mac-address") val macAddress: String?,
        @param:Json(name = "on-interface") val onInterface: String?
    )

    @Test
    fun `parse bridge host and assert mac and on-interface present`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/bridge_host.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, GoldenBridgeHost::class.java)
        val adapter = moshi.adapter<List<GoldenBridgeHost>>(type)

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertTrue(parsed!!.any { it.macAddress == "BC:C7:46:9C:FC:E4" && it.onInterface == "wifi1" })
    }
}

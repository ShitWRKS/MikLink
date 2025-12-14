package com.app.miklink.data.remote.mikrotik.golden

import com.app.miklink.testsupport.FixtureLoader
import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test

class BridgeVlanGoldenParsingTest {
    @Test
    fun `parse bridge vlan empty returns empty list`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/bridge_vlan_empty.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, Any::class.java)
        val adapter = moshi.adapter<List<Any>>(type)

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertTrue(parsed!!.isEmpty())
    }
}

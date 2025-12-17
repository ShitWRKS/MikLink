/*
 * Purpose: Guard Moshi serialization/parsing for MikroTik DTOs with hyphenated keys used in RouterOS REST calls.
 * Inputs: RouteAdd and CableTestResult DTOs serialized/parsing with TestMoshiProvider.
 * Outputs: Assertions that ensure correct key names (e.g., "dst-address", "cable-pairs") and non-null mapping.
 * Notes: Prevents regressions that would trigger HTTP 400 or null fields when RouterOS expects hyphenated JSON.
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MikroTikJsonAdaptersTest {

    @Test
    fun `route add serializes dst-address key`() {
        val moshi = TestMoshiProvider.provideMoshi()
        val adapter = moshi.adapter(RouteAdd::class.java)

        val json = adapter.toJson(RouteAdd(dstAddress = "0.0.0.0/0", gateway = "192.168.0.1", comment = "MikLink_Auto"))

        assertTrue(json.contains("\"dst-address\":\"0.0.0.0/0\""))
    }

    @Test
    fun `parse cable-pairs into dto`() {
        val json = """
            [
                {
                    "cable-pairs": [
                        { "pair": "A", "status": "ok" }
                    ],
                    "status": "link-ok"
                }
            ]
        """.trimIndent()

        val moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, CableTestResult::class.java)
        val adapter = moshi.adapter<List<CableTestResult>>(type)

        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        val item = parsed!!.first()
        assertEquals("link-ok", item.status)
        assertEquals("ok", item.cablePairs?.first()?.get("status"))
    }
}

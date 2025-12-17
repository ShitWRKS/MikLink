/*
 * Purpose: Confirm Moshi parsing of system resource golden fixture for board metadata.
 * Inputs: RouterOS 7.20.5 system_resource_hap_ax2.json and default Moshi adapter.
 * Outputs: Assertions on boardName and version fields.
 */
package com.app.miklink.data.remote.mikrotik.golden

import com.app.miklink.testsupport.FixtureLoader
import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Json
import org.junit.Assert.*
import org.junit.Test

class SystemResourceGoldenParsingTest {

    data class GoldenSystemResource(
        @param:Json(name = "board-name") val boardName: String?,
        val version: String?
    )

    @Test
    fun `parse system resource golden fixture`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/system_resource_hap_ax2.json")
        val moshi = TestMoshiProvider.provideMoshi()
        val adapter = moshi.adapter(GoldenSystemResource::class.java)

        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals("hAP ax^2", parsed?.boardName)
        assertEquals("7.20.5 (stable)", parsed?.version)
    }
}

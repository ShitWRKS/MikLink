package com.app.miklink.data.json

import com.app.miklink.testsupport.TestMoshiProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedHashMapJsonAdapterFactoryTest {

    private data class Payload(
        val g: LinkedHashMap<String, String>
    )

    @Test
    fun `parses and serializes concrete linked hash map fields`() {
        val adapter = TestMoshiProvider.provideMoshi().adapter(Payload::class.java)
        val parsed = adapter.fromJson("""{"g":{"status":"PASS","mode":"DHCP"}}""")

        assertNotNull(parsed)
        val nonNull = parsed ?: error("parsed payload is null")
        assertEquals("PASS", nonNull.g["status"])
        assertEquals("DHCP", nonNull.g["mode"])

        val encoded = adapter.toJson(Payload(linkedMapOf("x" to "1", "y" to "2")))
        assertTrue(encoded.contains("\"x\":\"1\""))
        assertTrue(encoded.contains("\"y\":\"2\""))
    }
}

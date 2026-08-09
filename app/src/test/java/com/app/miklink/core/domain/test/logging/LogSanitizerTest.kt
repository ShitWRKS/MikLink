/*
 * Purpose: Ensure LogSanitizer redacts sensitive tokens and truncates overly long log lines.
 * Inputs: Raw log strings containing secrets or excessive length.
 * Outputs: Sanitized strings with redaction tokens and truncated suffix markers.
 */
package com.app.miklink.core.domain.test.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {

    private val sanitizer = LogSanitizer()

    @Test
    fun `redacts password and tokens`() {
        val raw = "password=hunter2 token=abcd probePassword=secret"
        val sanitized = sanitizer.sanitize(raw)
        assertEquals("password=<redacted> token=<redacted> probePassword=<redacted>", sanitized)
    }

    @Test
    fun `redacts authorization header`() {
        val raw = "Authorization: Bearer 123456"
        val sanitized = sanitizer.sanitize(raw)
        assertEquals("Authorization: <redacted>", sanitized)
    }

    @Test
    fun `truncates long lines`() {
        val long = buildString {
            repeat(510) { append('x') }
        }
        val sanitized = sanitizer.sanitize(long)
        assertEquals(500 + " ...[truncated]".length, sanitized.length)
        assertTrue(sanitized.endsWith("...[truncated]"))
    }

    @Test
    fun `recursively redacts nested secret fields`() {
        val raw = mapOf(
            "request" to mapOf(
                "headers" to listOf(
                    mapOf("Authorization" to "Basic dXNlcjpwYXNz"),
                    mapOf("safe" to "visible")
                ),
                "password" to "hunter2"
            )
        )

        val sanitized = sanitizer.sanitizeValue(raw) as Map<*, *>
        val request = sanitized["request"] as Map<*, *>
        val headers = request["headers"] as List<*>

        assertEquals("<redacted>", request["password"])
        assertEquals("<redacted>", (headers[0] as Map<*, *>)["Authorization"])
        assertEquals("visible", (headers[1] as Map<*, *>)["safe"])
    }

    @Test
    fun `redacts secret assignments embedded in innocent string values`() {
        val sanitized = sanitizer.sanitizeValue(
            mapOf("payload" to "{\"password\":\"hunter2\",\"token\":\"abcd\"}")
        ).toString()

        assertFalse(sanitized.contains("hunter2"))
        assertFalse(sanitized.contains("abcd"))
        assertTrue(sanitized.contains("<redacted>"))
    }

    @Test
    fun `bounds structured string values`() {
        val sanitized = sanitizer.sanitizeValue(mapOf("raw" to "x".repeat(510))) as Map<*, *>
        val raw = sanitized["raw"] as String

        assertEquals(500 + " ...[truncated]".length, raw.length)
        assertTrue(raw.endsWith("...[truncated]"))
    }
}

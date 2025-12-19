/*
 * Purpose: Validate ProbeErrorMapper translates connectivity exceptions into actionable messages.
 * Inputs: SSLHandshakeException and generic exceptions.
 * Outputs: User-facing error strings that distinguish TLS handshake issues from generic failures.
 * Notes: Prevents regressions where HTTPS failures become opaque or misleading.
 */
package com.app.miklink.data.repository.mikrotik

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.net.ssl.SSLHandshakeException

class ProbeErrorMapperTest {
    @Test
    fun `maps ssl handshake to explicit https message`() {
        val error = SSLHandshakeException("handshake failure")

        val message = ProbeErrorMapper.toMessage(
            error = error,
            defaultMessage = "fallback",
            handshakeMessage = "HTTPS handshake failed: guidance"
        )

        assertEquals("HTTPS handshake failed: guidance", message)
    }

    @Test
    fun `falls back to default when message missing`() {
        val error = RuntimeException(null as String?)

        val message = ProbeErrorMapper.toMessage(
            error = error,
            defaultMessage = "fallback",
            handshakeMessage = "handshake"
        )

        assertTrue(message == "fallback")
    }
}

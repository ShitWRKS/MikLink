/*
 * Purpose: Validate ProbeErrorMapper translates typed TestError into actionable messages.
 * Inputs: TestError.Tls and generic TestError variants.
 * Outputs: User-facing error strings that distinguish TLS failures from generic failures.
 * Notes: Prevents regressions where HTTPS failures become opaque or misleading.
 */
package com.app.miklink.data.repository.mikrotik

import com.app.miklink.core.domain.test.model.TestError
import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeErrorMapperTest {
    @Test
    fun `maps tls error to explicit https message`() {
        val error = TestError.Tls("handshake failure")

        val message = ProbeErrorMapper.toMessage(
            error = error,
            defaultMessage = "fallback",
            handshakeMessage = "HTTPS handshake failed: guidance"
        )

        assertEquals("HTTPS handshake failed: guidance", message)
    }

    @Test
    fun `falls back to default when message is blank`() {
        val error = TestError.Unexpected("")

        val message = ProbeErrorMapper.toMessage(
            error = error,
            defaultMessage = "fallback",
            handshakeMessage = "handshake"
        )

        assertEquals("fallback", message)
    }
}

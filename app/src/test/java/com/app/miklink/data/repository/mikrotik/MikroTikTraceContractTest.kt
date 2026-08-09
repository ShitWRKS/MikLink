package com.app.miklink.data.repository.mikrotik

import com.app.miklink.core.domain.test.logging.LogSanitizer
import com.app.miklink.core.domain.test.logging.ProbeTraceContract
import com.app.miklink.core.domain.test.logging.ProbeTracePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MikroTikTraceContractTest {
    private val base = ProbeTracePoint(
        schemaVersion = "1.0.0",
        sessionId = "session",
        scenarioId = "live-ping",
        operationId = "PING",
        exchangeId = "exchange",
        eventType = "probe_request"
    )

    @Test
    fun requestAndResponseKeepTheSameCorrelation() {
        ProbeTraceContract.requireValid(listOf(base, base.copy(eventType = "probe_response")))
    }

    @Test
    fun probeResponseKeepsTheLegacyRunnerAlias() {
        assertEquals(
            listOf("probe_response", "mikrotik_raw_response"),
            compatibleTraceEventTypes("probe_response")
        )
        assertEquals(listOf("parsed_response"), compatibleTraceEventTypes("parsed_response"))
    }

    @Test
    fun requestAndErrorKeepTheSameCorrelationAndSanitizeSecrets() {
        val sanitizer = LogSanitizer()
        val error = base.copy(
            eventType = "probe_error",
            payload = sanitizer.sanitizeValue(mapOf("password" to "canary-secret", "message" to "failed")) as Map<String, Any?>
        )
        ProbeTraceContract.requireValid(listOf(base, error))
        assertTrue(error.payload.toString().contains("<redacted>"))
        assertTrue(!error.payload.toString().contains("canary-secret"))
    }

    @Test
    fun missingTerminalOrChangedCorrelationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ProbeTraceContract.requireValid(listOf(base))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProbeTraceContract.requireValid(
                listOf(base, base.copy(sessionId = "other", eventType = "probe_response"))
            )
        }
    }
}

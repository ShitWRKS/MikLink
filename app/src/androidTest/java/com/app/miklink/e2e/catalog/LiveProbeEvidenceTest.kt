package com.app.miklink.e2e.catalog

import androidx.test.platform.app.InstrumentationRegistry
import com.app.miklink.core.domain.test.logging.ProbeTraceContract
import com.app.miklink.core.domain.test.logging.ProbeTracePoint
import com.app.miklink.e2e.support.ArtifactSecretScanner
import com.app.miklink.e2e.support.EvidenceWriter
import com.app.miklink.e2e.support.TraceEvent
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveProbeEvidenceTest {
    @Test
    fun successfulExchangeHasCompleteCorrelatedChainAndSecretFreeEvidence() {
        val eventTypes = listOf(
            "probe_request",
            "probe_response",
            "parsed_response",
            "normalized_response",
            "threshold_evaluation",
            "test_decision",
            "ui_snapshot"
        )
        val points = eventTypes.map { type ->
            ProbeTracePoint(
                schemaVersion = "1.0.0",
                sessionId = "session",
                scenarioId = "live-ping",
                operationId = "PING",
                exchangeId = "exchange",
                eventType = type,
                payload = mapOf("value" to "safe")
            )
        }
        assertTrue(ProbeTraceContract.fullChainViolations(points).isEmpty())

        val root = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "live-evidence-${System.nanoTime()}"
        )
        val writer = EvidenceWriter(root)
        var traceFile: File? = null
        points.forEach { point ->
            traceFile = writer.appendTrace(
                "live-ping",
                TraceEvent(
                    timestamp = "2026-08-09T10:00:00Z",
                    sessionId = point.sessionId,
                    scenarioId = point.scenarioId,
                    operationId = point.operationId,
                    exchangeId = point.exchangeId,
                    eventType = point.eventType,
                    payload = point.payload
                )
            )
        }
        try {
            assertTrue(requireNotNull(traceFile).isFile)
            assertTrue(ArtifactSecretScanner(setOf("credential-canary-never-written")).scan(listOf(traceFile)).isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun failedExchangeEndsWithCorrelatedErrorWithoutClaimingSuccessChain() {
        val request = ProbeTracePoint("1.0.0", "session", "live-link", "LINK", "exchange", "probe_request")
        val error = request.copy(eventType = "probe_error", payload = mapOf("message" to "sanitized"))
        assertTrue(ProbeTraceContract.violations(listOf(request, error)).isEmpty())
    }
}

package com.app.miklink.core.domain.test.logging

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

data class DebugTraceCorrelation(
    val runId: String,
    val sessionId: String = runId,
    val scenarioId: String = "ui-run",
    val operationId: String? = null,
    val exchangeId: String? = null
)

/**
 * Holds the run ID of the currently active test run for trace attribution.
 *
 * Clearing is generation-aware (ADR-0013): only the owner of the expected run ID can clear the
 * context, so a stale/finished run cannot wipe the trace context of a newer run.
 */
@Singleton
class DebugTraceRunContext @Inject constructor() {
    private val currentCorrelation = AtomicReference<DebugTraceCorrelation?>(null)

    fun set(runId: String) {
        currentCorrelation.updateAndGet { previous ->
            DebugTraceCorrelation(
                runId = runId,
                sessionId = previous?.sessionId ?: runId,
                scenarioId = previous?.scenarioId ?: "ui-run"
            )
        }
    }

    fun set(correlation: DebugTraceCorrelation) {
        currentCorrelation.set(correlation)
    }

    /**
     * Clears the context only if it still holds [expectedRunId].
     * Returns true when the context was actually cleared (ownership held), false otherwise.
     */
    fun clear(expectedRunId: String): Boolean {
        while (true) {
            val current = currentCorrelation.get() ?: return false
            if (current.runId != expectedRunId) return false
            if (currentCorrelation.compareAndSet(current, null)) return true
        }
    }

    fun current(): String? = currentCorrelation.get()?.runId

    fun correlation(): DebugTraceCorrelation? = currentCorrelation.get()

    fun withOperation(operationId: String, exchangeId: String): DebugTraceCorrelation? {
        while (true) {
            val current = currentCorrelation.get() ?: return null
            val updated = current.copy(operationId = operationId, exchangeId = exchangeId)
            if (currentCorrelation.compareAndSet(current, updated)) return updated
        }
    }

    fun clearOperation(expectedExchangeId: String): Boolean {
        while (true) {
            val current = currentCorrelation.get() ?: return false
            if (current.exchangeId != expectedExchangeId) return false
            val updated = current.copy(operationId = null, exchangeId = null)
            if (currentCorrelation.compareAndSet(current, updated)) return true
        }
    }
}

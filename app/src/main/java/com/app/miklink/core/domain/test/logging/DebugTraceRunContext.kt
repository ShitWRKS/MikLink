package com.app.miklink.core.domain.test.logging

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the run ID of the currently active test run for trace attribution.
 *
 * Clearing is generation-aware (ADR-0013): only the owner of the expected run ID can clear the
 * context, so a stale/finished run cannot wipe the trace context of a newer run.
 */
@Singleton
class DebugTraceRunContext @Inject constructor() {
    private val currentRunId = AtomicReference<String?>(null)

    fun set(runId: String) {
        currentRunId.set(runId)
    }

    /**
     * Clears the context only if it still holds [expectedRunId].
     * Returns true when the context was actually cleared (ownership held), false otherwise.
     */
    fun clear(expectedRunId: String): Boolean =
        currentRunId.compareAndSet(expectedRunId, null)

    fun current(): String? = currentRunId.get()
}

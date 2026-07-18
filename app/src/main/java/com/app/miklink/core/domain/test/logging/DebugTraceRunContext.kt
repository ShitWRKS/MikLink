package com.app.miklink.core.domain.test.logging

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugTraceRunContext @Inject constructor() {
    private val currentRunId = AtomicReference<String?>(null)

    fun set(runId: String) {
        currentRunId.set(runId)
    }

    fun clear() {
        currentRunId.set(null)
    }

    fun current(): String? = currentRunId.get()
}

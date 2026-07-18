package com.app.miklink.core.domain.test.logging

interface DebugTraceSink {
    fun startRun(source: String, fields: Map<String, Any?> = emptyMap()): String
    fun event(runId: String, event: String, fields: Map<String, Any?> = emptyMap())
    fun finishRun(runId: String, finalStatus: String, fields: Map<String, Any?> = emptyMap())
}

object NoOpDebugTraceSink : DebugTraceSink {
    override fun startRun(source: String, fields: Map<String, Any?>): String = "noop"
    override fun event(runId: String, event: String, fields: Map<String, Any?>) = Unit
    override fun finishRun(runId: String, finalStatus: String, fields: Map<String, Any?>) = Unit
}

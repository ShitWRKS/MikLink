package com.app.miklink.core.domain.test.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugTraceSinkImpl @Inject constructor() : DebugTraceSink {
    override fun startRun(source: String, fields: Map<String, Any?>): String = "release-noop"
    override fun event(runId: String, event: String, fields: Map<String, Any?>) = Unit
    override fun finishRun(runId: String, finalStatus: String, fields: Map<String, Any?>) = Unit
}

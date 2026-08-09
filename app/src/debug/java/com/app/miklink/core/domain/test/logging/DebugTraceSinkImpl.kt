package com.app.miklink.core.domain.test.logging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DebugTraceSinkImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DebugTraceSink {
    private data class RunState(
        val file: File,
        val sessionId: String,
        val scenarioId: String,
        val sequence: AtomicLong = AtomicLong(0)
    )

    private val runs = ConcurrentHashMap<String, RunState>()
    private val sanitizer = LogSanitizer(maxLength = MAX_TRACE_VALUE_LENGTH)

    override fun startRun(source: String, fields: Map<String, Any?>): String {
        require(source.isNotBlank()) { "Trace source is required" }
        val runId = UUID.randomUUID().toString()
        val baseDir = File(context.getExternalFilesDir(null), TRACE_DIRECTORY)
        check(baseDir.exists() || baseDir.mkdirs()) { "Unable to create debug trace directory" }
        val runFile = File(baseDir, "debug_trace_$runId.ndjson")
        val sessionId = fields["sessionId"]?.toString()?.takeIf { it.isNotBlank() } ?: runId
        val scenarioId = fields["scenarioId"]?.toString()?.takeIf { it.isNotBlank() } ?: source
        runs[runId] = RunState(runFile, sessionId, scenarioId)
        Log.i(E2E_TAG, """MIKLINK_E2E_TRACE {"runId":"$runId","path":"${runFile.absolutePath}"}""")
        return runId
    }

    override fun event(runId: String, event: String, fields: Map<String, Any?>) {
        val state = runs[runId] ?: return
        require(event in SUPPORTED_EVENT_TYPES) { "Unsupported trace event type: $event" }
        val sequence = state.sequence.incrementAndGet()
        val sessionId = fields["sessionId"]?.toString()?.takeIf { it.isNotBlank() } ?: state.sessionId
        val scenarioId = fields["scenarioId"]?.toString()?.takeIf { it.isNotBlank() } ?: state.scenarioId
        val operationId = fields["operationId"]?.toString()?.takeIf { it.isNotBlank() }
        val exchangeId = fields["exchangeId"]?.toString()?.takeIf { it.isNotBlank() }
        val payloadFields = fields - CORRELATION_KEYS
        val safePayload = sanitizer.sanitizeValue(payloadFields)

        val payload = JSONObject()
            .put("schemaVersion", TRACE_SCHEMA_VERSION)
            .put("timestamp", Instant.now().toString())
            .put("sessionId", sessionId)
            .put("scenarioId", scenarioId)
            .put("operationId", operationId ?: JSONObject.NULL)
            .put("exchangeId", exchangeId ?: JSONObject.NULL)
            .put("eventType", event)
            .put("payload", toJsonValue(safePayload))
            // Transitional aliases retained until both host runners pass parity.
            .put("runId", runId)
            .put("seq", sequence)
            .put("event", event)

        synchronized(state) {
            state.file.appendText(payload.toString() + "\n")
        }
    }

    override fun finishRun(runId: String, finalStatus: String, fields: Map<String, Any?>) {
        event(runId, "run_finished", fields + ("finalStatus" to finalStatus))
        runs.remove(runId)
    }

    private fun toJsonValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().also { json ->
            value.forEach { (key, child) -> if (key != null) json.put(key.toString(), toJsonValue(child)) }
        }
        is Iterable<*> -> JSONArray().also { json -> value.forEach { json.put(toJsonValue(it)) } }
        is Array<*> -> JSONArray().also { json -> value.forEach { json.put(toJsonValue(it)) } }
        is Number, is Boolean, is String -> value
        else -> sanitizer.sanitize(value.toString())
    }

    private companion object {
        private const val TRACE_SCHEMA_VERSION = "1.0.0"
        private const val TRACE_DIRECTORY = "e2e-trace"
        private const val E2E_TAG = "MIKLINK_E2E"
        private const val MAX_TRACE_VALUE_LENGTH = 16_384
        private val CORRELATION_KEYS = setOf("sessionId", "scenarioId", "operationId", "exchangeId")
        private val SUPPORTED_EVENT_TYPES = setOf(
            "run_started",
            "run_finished",
            "profile_loaded",
            "test_enabled_state",
            "thresholds_loaded",
            "scenario_started",
            "prerequisite",
            "step_started",
            "step_finished",
            "mikrotik_request",
            "mikrotik_raw_response",
            "probe_request",
            "probe_response",
            "probe_error",
            "probe_exchange_completed",
            "parsed_response",
            "normalized_response",
            "normalized_result",
            "threshold_evaluation",
            "test_decision",
            "ui_snapshot",
            "technical_error",
            "cleanup",
            "scenario_finished"
        )
    }
}

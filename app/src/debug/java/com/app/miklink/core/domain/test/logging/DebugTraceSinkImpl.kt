package com.app.miklink.core.domain.test.logging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
        val sequence: AtomicLong = AtomicLong(0)
    )

    private val runs = ConcurrentHashMap<String, RunState>()

    override fun startRun(source: String, fields: Map<String, Any?>): String {
        val runId = UUID.randomUUID().toString()
        val baseDir = File(context.getExternalFilesDir(null), "e2e-trace")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        val runFile = File(baseDir, "debug_trace_$runId.ndjson")
        runs[runId] = RunState(file = runFile)
        Log.i(E2E_TAG, """MIKLINK_E2E_TRACE {"runId":"$runId","path":"${runFile.absolutePath}"}""")
        return runId
    }

    override fun event(runId: String, event: String, fields: Map<String, Any?>) {
        val state = runs[runId] ?: return
        val seq = state.sequence.incrementAndGet()
        val payload = JSONObject()
            .put("runId", runId)
            .put("seq", seq)
            .put("event", event)

        fields.forEach { (key, value) ->
            payload.put(key, toJsonValue(key, value))
        }

        state.file.appendText(payload.toString() + "\n")
    }

    override fun finishRun(runId: String, finalStatus: String, fields: Map<String, Any?>) {
        event(
            runId = runId,
            event = "run_finished",
            fields = fields + ("finalStatus" to finalStatus)
        )
        runs.remove(runId)
    }

    private fun toJsonValue(key: String, value: Any?): Any? {
        if (value == null) return JSONObject.NULL
        if (shouldRedact(key)) return REDACTED_VALUE

        return when (value) {
            is Map<*, *> -> {
                val json = JSONObject()
                value.entries.forEach { (childKey, childValue) ->
                    if (childKey != null) {
                        val childName = childKey.toString()
                        json.put(childName, toJsonValue(childName, childValue))
                    }
                }
                json
            }
            is Iterable<*> -> {
                val json = JSONArray()
                value.forEach { element ->
                    json.put(toJsonValue(key, element))
                }
                json
            }
            is Array<*> -> {
                val json = JSONArray()
                value.forEach { element ->
                    json.put(toJsonValue(key, element))
                }
                json
            }
            is Number, is Boolean, is String -> value
            else -> value.toString()
        }
    }

    private fun shouldRedact(key: String): Boolean {
        val normalized = key.lowercase()
        return normalized.contains("password") ||
            normalized.contains("token") ||
            normalized.contains("secret") ||
            normalized.contains("authorization") ||
            normalized.contains("cookie") ||
            normalized.contains("privatekey") ||
            normalized.contains("private_key")
    }

    private companion object {
        private const val E2E_TAG = "MIKLINK_E2E"
        private const val REDACTED_VALUE = "<redacted>"
    }
}

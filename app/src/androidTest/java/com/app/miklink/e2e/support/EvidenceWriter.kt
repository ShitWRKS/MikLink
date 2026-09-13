package com.app.miklink.e2e.support

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class EvidenceWriter(
    private val root: File,
    moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) {
    private val scenarioAdapter: JsonAdapter<ScenarioResult> =
        moshi.adapter(ScenarioResult::class.java).serializeNulls().indent("  ")
    private val manifestAdapter: JsonAdapter<SessionManifest> =
        moshi.adapter(SessionManifest::class.java).serializeNulls().indent("  ")
    private val traceAdapter: JsonAdapter<TraceEvent> =
        moshi.adapter(TraceEvent::class.java).serializeNulls()

    fun writeScenarioResult(result: ScenarioResult): File {
        val destination = containedFile("scenarios/${safeSegment(result.scenarioId)}/scenario-result.json")
        atomicWrite(destination, scenarioAdapter.toJson(result))
        return destination
    }

    fun writeManifest(manifest: SessionManifest): File {
        val destination = containedFile("session-manifest.json")
        atomicWrite(destination, manifestAdapter.toJson(manifest))
        return destination
    }

    fun readManifest(): SessionManifest? {
        val source = containedFile("session-manifest.json")
        if (!source.isFile) return null
        return manifestAdapter.fromJson(source.readText(StandardCharsets.UTF_8))
    }

    fun writeProgress(sessionId: String, scenarioId: String, lastStepId: String?): File {
        val destination = containedFile("scenarios/${safeSegment(scenarioId)}/scenario-progress.json")
        val json = """{"schemaVersion":"$EVIDENCE_SCHEMA_VERSION","sessionId":"${escape(sessionId)}","scenarioId":"${escape(scenarioId)}","lastSuccessfulStepId":${lastStepId?.let { "\"${escape(it)}\"" } ?: "null"}}"""
        atomicWrite(destination, json)
        return destination
    }

    fun appendTrace(scenarioId: String, event: TraceEvent): File {
        val destination = containedFile("scenarios/${safeSegment(scenarioId)}/probe-trace.ndjson")
        destination.parentFile?.mkdirs()
        destination.appendText(traceAdapter.toJson(event) + "\n", StandardCharsets.UTF_8)
        return destination
    }

    private fun atomicWrite(destination: File, content: String) {
        destination.parentFile?.mkdirs()
        val temp = File(destination.parentFile, "${destination.name}.tmp")
        FileOutputStream(temp).use { stream ->
            stream.write(content.toByteArray(StandardCharsets.UTF_8))
            stream.fd.sync()
        }
        try {
            Files.move(
                temp.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun containedFile(relativePath: String): File {
        require(!relativePath.startsWith('/') && !relativePath.contains("..")) { "Unsafe evidence path" }
        val file = File(root, relativePath)
        require(file.canonicalPath.startsWith(root.canonicalPath + File.separator)) { "Evidence escaped root" }
        return file
    }

    private fun safeSegment(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "unknown" }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}

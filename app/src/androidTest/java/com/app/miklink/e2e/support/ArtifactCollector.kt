package com.app.miklink.e2e.support

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.ResultsReporter
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.security.MessageDigest

class ArtifactCollector(
    private val sessionId: String,
    private val scenarioId: String
) {
    private val reporter = ResultsReporter(scenarioId)
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val references = mutableListOf<ArtifactReference>()

    fun captureScreenshot(
        filename: String = "screenshot.png",
        title: String = "MikLink current UI"
    ): ArtifactReference {
        val output = reporter.addNewFile(safeName(filename), title)
        check(device.takeScreenshot(output)) { "Unable to capture screenshot" }
        return register(output, filename, "image/png", RedactionStatus.NOT_REQUIRED)
    }

    fun captureHierarchy(
        filename: String = "ui-hierarchy.xml",
        title: String = "MikLink UI hierarchy"
    ): ArtifactReference {
        val output = reporter.addNewFile(safeName(filename), title)
        device.dumpWindowHierarchy(output)
        return register(output, filename, "application/xml", RedactionStatus.SANITIZED)
    }

    fun copyArtifact(
        source: File,
        filename: String,
        title: String,
        mediaType: String,
        redactionStatus: RedactionStatus
    ): ArtifactReference {
        require(source.isFile) { "Artifact source is missing: ${source.name}" }
        val output = reporter.addNewFile(safeName(filename), title)
        source.copyTo(output, overwrite = true)
        return register(output, filename, mediaType, redactionStatus)
    }

    fun writeRetrievalInstructions(devicePath: String): ArtifactReference {
        val output = reporter.addNewFile("adb-retrieval.txt", "Direct adb retrieval fallback")
        output.writeText(
            "session=$sessionId\nscenario=$scenarioId\ndevicePath=$devicePath\n" +
                "Use: adb -s <serial> pull <devicePath> <session-dir>\n"
        )
        return register(output, "adb-retrieval.txt", "text/plain", RedactionStatus.SANITIZED)
    }

    fun finish(): List<ArtifactReference> {
        reporter.reportToInstrumentation()
        return references.toList()
    }

    private fun register(
        file: File,
        logicalName: String,
        mediaType: String,
        redactionStatus: RedactionStatus
    ): ArtifactReference = ArtifactReference(
        path = "scenarios/${safeName(scenarioId)}/${safeName(logicalName)}",
        mediaType = mediaType,
        sizeBytes = file.length(),
        sha256 = file.sha256(),
        redactionStatus = redactionStatus
    ).also(references::add)

    private fun safeName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "artifact" }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

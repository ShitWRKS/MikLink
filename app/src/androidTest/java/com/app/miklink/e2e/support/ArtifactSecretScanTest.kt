package com.app.miklink.e2e.support

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtifactSecretScanTest {
    @Test
    fun credentialCanaryIsDetectedAcrossEveryLiveArtifactType() {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "secret-scan-${System.nanoTime()}")
        root.mkdirs()
        val canary = "MIKLINK-CREDENTIAL-CANARY"
        val files = listOf("trace.ndjson", "scenario-result.json", "manifest.json", "ui.xml", "log.txt", "screenshot.png")
            .map { name -> File(root, name).apply { writeBytes("prefix-$canary-suffix".toByteArray()) } }
        try {
            val findings = ArtifactSecretScanner(setOf(canary)).scan(files)
            assertEquals(files.map { it.name }.toSet(), findings.map { it.file }.toSet())
        } finally {
            files.forEach { it.delete() }
            root.delete()
        }
    }

    @Test
    fun sanitizedArtifactsHaveZeroFindings() {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "secret-clean-${System.nanoTime()}")
        root.mkdirs()
        val file = File(root, "trace.ndjson").apply { writeText("{\"password\":\"[REDACTED]\"}") }
        try {
            assertTrue(ArtifactSecretScanner(setOf("actual-password")).scan(listOf(file)).isEmpty())
        } finally {
            file.delete()
            root.delete()
        }
    }
}

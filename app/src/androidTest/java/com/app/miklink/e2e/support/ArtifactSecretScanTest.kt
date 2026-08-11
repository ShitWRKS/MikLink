package com.app.miklink.e2e.support

import androidx.test.platform.app.InstrumentationRegistry
import com.app.miklink.e2e.catalog.appOnlyDependencies
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class ArtifactSecretScanTest {
    @Test
    fun retainedAcceptanceArtifactsExcludeConfiguredProbeCredentials() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val sessionIds = InstrumentationRegistry.getArguments().getString("secretScanSessionIds")
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
        assumeTrue("secretScanSessionIds was not supplied", sessionIds.isNotEmpty())

        val probe = runBlocking { appOnlyDependencies().probeRepository().getProbeConfig() }
        val credentialCanaries = setOfNotNull(probe?.username, probe?.password)
            .filter(String::isNotBlank)
            .toSet()
        assumeTrue("No configured credential canary is available", credentialCanaries.isNotEmpty())

        val evidenceRoot = File(requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "agent-tests")
        val files = sessionIds.flatMap { sessionId ->
            File(evidenceRoot, sessionId).walkTopDown().filter(File::isFile).toList()
        }
        assertTrue("No acceptance artifacts were found for the requested sessions", files.isNotEmpty())
        val findings = ArtifactSecretScanner(credentialCanaries).scan(files)
        assertTrue(
            "Configured probe credentials reached acceptance artifacts: ${findings.map { it.file }.distinct()}",
            findings.isEmpty()
        )
    }

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

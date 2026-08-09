package com.app.miklink.quality

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseIsolationScanTest {
    private val root: Path = sequenceOf(Paths.get(""), Paths.get("..")).map { it.toAbsolutePath().normalize() }
        .first { it.resolve("app/build.gradle.kts").exists() }

    @Test
    fun agentEntryPointAndSemanticActivationExistOnlyInDebugSourceSet() {
        val debugEntry = root.resolve("app/src/debug/java/com/app/miklink/e2e/DebugE2EEntryPoint.kt")
        assertTrue("Debug entry point is missing", debugEntry.exists())
        assertFalse(root.resolve("app/src/main/java/com/app/miklink/e2e/DebugE2EEntryPoint.kt").exists())
        assertFalse(root.resolve("app/src/release/java/com/app/miklink/e2e/DebugE2EEntryPoint.kt").exists())

        val debugPolicy = read("app/src/debug/java/com/app/miklink/ui/testing/AgentSemanticsConfig.kt")
        val releasePolicy = read("app/src/release/java/com/app/miklink/ui/testing/AgentSemanticsConfig.kt")
        assertTrue(debugPolicy.contains("enabled: Boolean = true"))
        assertTrue(debugPolicy.contains("testTagsAsResourceId = true"))
        assertTrue(releasePolicy.contains("enabled: Boolean = false"))
        assertFalse(releasePolicy.contains("testTagsAsResourceId"))
        assertFalse(releasePolicy.contains("semantics"))
    }

    @Test
    fun releaseTraceImplementationIsAWriteFreeNoOp() {
        val releaseSink = read("app/src/release/java/com/app/miklink/core/domain/test/logging/DebugTraceSinkImpl.kt")
        val forbidden = listOf("File(", "appendText", "writeText", "Log.", "externalFilesDir", "MIKLINK_E2E_TRACE")
        assertEquals(forbidden.associateWith { releaseSink.contains(it) }.filterValues { it }, emptyMap<String, Boolean>())
        assertTrue(releaseSink.contains("override fun event") && releaseSink.contains("= Unit"))
        assertTrue(releaseSink.contains("override fun finishRun") && releaseSink.contains("= Unit"))
    }

    @Test
    fun manifestsExposeNoAgentOrTestControlComponent() {
        val manifests = listOf("app/src/main/AndroidManifest.xml", "app/src/release/AndroidManifest.xml")
            .map(root::resolve)
            .filter(Files::exists)
        val forbidden = Regex("agent|e2e|test.?control|debug.?trace", RegexOption.IGNORE_CASE)
        val violations = manifests.flatMap { manifest ->
            Files.readAllLines(manifest).mapIndexedNotNull { index, line ->
                if (forbidden.containsMatchIn(line)) "${root.relativize(manifest)}:${index + 1}: $line" else null
            }
        }
        assertEquals("Forbidden release manifest surface:\n${violations.joinToString("\n")}", emptyList<String>(), violations)
    }

    @Test
    fun noRuntimeFlagIntentArgumentOrSettingCanActivateAgentMode() {
        val forbidden = listOf(
            "agentMode", "agent_mode", "enableAgent", "enable_agent", "e2eMode", "e2e_mode",
            "testControl", "test_control", "allowWifiDisruption", "disposableLocalState",
            "testTagsAsResourceId"
        )
        val roots = listOf(
            root.resolve("app/src/main"),
            root.resolve("app/src/release"),
            root.resolve("app/build.gradle.kts")
        )
        val files = roots.flatMap { path ->
            if (Files.isRegularFile(path)) listOf(path)
            else Files.walk(path).use { stream ->
                stream.filter(Files::isRegularFile)
                    .filter { it.toString().endsWith(".kt") || it.toString().endsWith(".xml") || it.toString().endsWith(".kts") }
                    .toList()
            }
        }.filterNot { it.toString().replace('\\', '/').endsWith("/AgentSemanticsConfig.kt") }
        val violations = files.flatMap { file ->
            val source = Files.readString(file)
            forbidden.filter(source::contains).map { token -> "${root.relativize(file)}: $token" }
        }
        assertEquals("Runtime activation surface found:\n${violations.joinToString("\n")}", emptyList<String>(), violations)
    }

    private fun read(relative: String): String = Files.readString(root.resolve(relative))
}

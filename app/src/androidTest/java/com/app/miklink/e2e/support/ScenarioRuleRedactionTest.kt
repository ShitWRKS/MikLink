package com.app.miklink.e2e.support

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class ScenarioRuleRedactionTest {
    @Test
    fun failureDetailIsSanitizedBeforeTheResultIsPersisted() {
        val root = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "scenario-rule-redaction-${System.nanoTime()}"
        )
        val secret = "credential-canary-${System.nanoTime()}"
        val rule = ScenarioRule(
            sessionId = "redaction-session",
            scenarioIdResolver = { "redaction-scenario" },
            evidenceRoot = root,
            timeoutMs = 10_000L
        )
        val failing = object : Statement() {
            override fun evaluate() {
                throw AssertionError("password=$secret")
            }
        }
        val wrapped = rule.apply(
            failing,
            Description.createTestDescription(javaClass, "syntheticFailure")
        )

        try {
            runCatching { wrapped.evaluate() }
            val resultFile = File(root, "scenarios/redaction-scenario/scenario-result.json")
            val manifestFile = File(root, "session-manifest.json")
            val resultText = resultFile.readText()
            val manifest = JSONObject(manifestFile.readText())

            assertFalse("A failure credential reached persisted evidence", resultText.contains(secret))
            assertTrue(resultText.contains("password=<redacted>"))
            assertEquals(
                "SANITIZED",
                manifest.getJSONArray("artifacts").getJSONObject(0).getString("redactionStatus")
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun copiedArtifactIsIndexedByTheResultAndManifest() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val root = File(cache, "scenario-rule-artifact-${System.nanoTime()}")
        val source = File(cache, "source-trace-${System.nanoTime()}.ndjson")
            .apply { writeText("{\"eventType\":\"run_finished\"}\n") }
        val rule = ScenarioRule(
            sessionId = "artifact-session",
            scenarioIdResolver = { "artifact-scenario" },
            evidenceRoot = root,
            timeoutMs = 10_000L
        )
        val passing = object : Statement() {
            override fun evaluate() {
                rule.copyArtifact(
                    source = source,
                    filename = "probe-trace.ndjson",
                    mediaType = "application/x-ndjson",
                    redactionStatus = RedactionStatus.VERIFIED_SCAN
                )
            }
        }

        try {
            rule.apply(
                passing,
                Description.createTestDescription(javaClass, "syntheticArtifact")
            ).evaluate()
            val relative = "scenarios/artifact-scenario/probe-trace.ndjson"
            val result = JSONObject(File(root, "scenarios/artifact-scenario/scenario-result.json").readText())
            val manifest = JSONObject(File(root, "session-manifest.json").readText())

            assertEquals(relative, result.getJSONArray("artifactPaths").getString(0))
            assertEquals(relative, manifest.getJSONArray("artifacts").getJSONObject(0).getString("path"))
            assertTrue(File(root, relative).isFile)
        } finally {
            source.delete()
            root.deleteRecursively()
        }
    }
}

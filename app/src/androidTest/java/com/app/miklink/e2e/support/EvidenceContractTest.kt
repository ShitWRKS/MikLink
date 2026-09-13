package com.app.miklink.e2e.support

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvidenceContractTest {
    @Test
    fun scenarioResultIsVersionedAndAtomicallyFinalized() {
        val root = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "evidence-contract-${System.nanoTime()}"
        )
        val writer = EvidenceWriter(root)
        val result = ScenarioResult(
            sessionId = "session-1",
            scenarioId = "contract-test",
            startedAt = "2026-08-09T10:00:00Z",
            endedAt = "2026-08-09T10:00:01Z",
            status = TerminalStatus.PASS,
            reasonCode = "ASSERTIONS_PASSED",
            prerequisites = emptyList(),
            steps = listOf(
                ScenarioStepResult(
                    stepId = "assert-visible",
                    kind = StepKind.ASSERTION,
                    status = StepStatus.PASS,
                    startedAt = "2026-08-09T10:00:00Z",
                    endedAt = "2026-08-09T10:00:01Z"
                )
            ),
            cleanup = CleanupResult(CleanupStatus.PASS)
        )

        val file = writer.writeScenarioResult(result)
        val json = JSONObject(file.readText())

        assertEquals(EVIDENCE_SCHEMA_VERSION, json.getString("schemaVersion"))
        assertEquals("PASS", json.getString("status"))
        assertEquals("contract-test", json.getString("scenarioId"))
        assertFalse(File(file.parentFile, "${file.name}.tmp").exists())
        assertTrue(file.canonicalPath.startsWith(root.canonicalPath))
    }

    @Test
    fun releaseManifestRejectsAgentScenariosTraceAndDestructivePolicy() {
        val releaseSmoke = validManifest(variant = "release", scenarioIds = listOf("release-smoke"))

        assertThrows(IllegalArgumentException::class.java) {
            releaseSmoke.copy(scenarioIds = listOf("client-crud"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            releaseSmoke.copy(
                artifacts = listOf(
                    validArtifact(
                        path = "scenarios/release-smoke/probe-trace.ndjson",
                        mediaType = "application/x-ndjson"
                    )
                )
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            releaseSmoke.copy(policy = SessionPolicy(disposableLocalState = true))
        }
    }

    @Test
    fun manifestRejectsUnknownVariantAndUncontainedArtifactPaths() {
        assertThrows(IllegalArgumentException::class.java) {
            validManifest(variant = "benchmark", scenarioIds = listOf("release-smoke"))
        }
        listOf("", "\\outside.txt", "../outside.txt", "scenarios/../outside.txt").forEach { path ->
            assertThrows("path=$path", IllegalArgumentException::class.java) {
                validManifest(artifacts = listOf(validArtifact(path = path)))
            }
        }
    }

    @Test
    fun manifestRejectsAnUnfinalizedSession() {
        assertThrows(IllegalArgumentException::class.java) {
            validManifest().copy(endedAt = null, cleanup = null)
        }
    }

    @Test
    fun scenarioResultRejectsInvalidTerminalClaimsAndArtifactPaths() {
        val valid = validScenarioResult()

        assertThrows(IllegalArgumentException::class.java) { valid.copy(sessionId = "") }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(reasonCode = "") }
        assertThrows(IllegalArgumentException::class.java) {
            valid.copy(
                steps = valid.steps + valid.steps.single().copy(
                    stepId = "failed-assertion",
                    status = StepStatus.FAIL
                )
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            valid.copy(status = TerminalStatus.NOT_RUN, reasonCode = "PREREQUISITE_MISSING")
        }
        assertThrows(IllegalArgumentException::class.java) {
            valid.copy(cleanup = CleanupResult(CleanupStatus.FAIL, "CLEANUP_FAILED"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            valid.copy(
                prerequisites = listOf(
                    PrerequisiteResult(
                        id = "probe",
                        required = true,
                        status = PrerequisiteStatus.UNAVAILABLE,
                        reasonCode = "PROBE_NOT_CONFIGURED"
                    )
                )
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            valid.copy(lastSuccessfulStepId = "missing-step")
        }
        listOf("", "/outside.png", "\\outside.png", "../outside.png").forEach { path ->
            assertThrows("path=$path", IllegalArgumentException::class.java) {
                valid.copy(artifactPaths = listOf(path))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            valid.copy(artifactPaths = listOf("screenshot.png", "screenshot.png"))
        }
    }

    private fun validManifest(
        variant: String = "debug",
        scenarioIds: List<String> = listOf("contract-test"),
        policy: SessionPolicy = SessionPolicy(),
        artifacts: List<ArtifactReference> = emptyList()
    ) = SessionManifest(
        sessionId = "session-1",
        startedAt = "2026-08-09T10:00:00Z",
        endedAt = "2026-08-09T10:00:01Z",
        build = BuildIdentity(
            applicationId = "com.app.miklink",
            versionCode = 1,
            versionName = "1.0",
            variant = variant,
            sourceRevision = "cd62906"
        ),
        device = DeviceIdentity(serial = "device", model = "model", apiLevel = 34),
        policy = policy,
        scenarioIds = scenarioIds,
        artifacts = artifacts,
        cleanup = CleanupResult(CleanupStatus.PASS)
    )

    private fun validArtifact(
        path: String,
        mediaType: String = "application/json"
    ) = ArtifactReference(
        path = path,
        mediaType = mediaType,
        sizeBytes = 1,
        sha256 = "a".repeat(64),
        redactionStatus = RedactionStatus.SANITIZED
    )

    private fun validScenarioResult() = ScenarioResult(
        sessionId = "session-1",
        scenarioId = "contract-test",
        startedAt = "2026-08-09T10:00:00Z",
        endedAt = "2026-08-09T10:00:01Z",
        status = TerminalStatus.PASS,
        reasonCode = "ASSERTIONS_PASSED",
        lastSuccessfulStepId = "assert-visible",
        prerequisites = emptyList(),
        steps = listOf(
            ScenarioStepResult(
                stepId = "assert-visible",
                kind = StepKind.ASSERTION,
                status = StepStatus.PASS,
                startedAt = "2026-08-09T10:00:00Z",
                endedAt = "2026-08-09T10:00:01Z"
            )
        ),
        cleanup = CleanupResult(CleanupStatus.PASS)
    )
}

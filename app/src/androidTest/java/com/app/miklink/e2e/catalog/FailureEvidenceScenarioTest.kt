package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.CleanupResult
import com.app.miklink.e2e.support.CleanupStatus
import com.app.miklink.e2e.support.PrerequisiteResult
import com.app.miklink.e2e.support.PrerequisiteStatus
import com.app.miklink.e2e.support.ProcessExitKind
import com.app.miklink.e2e.support.ProcessExitObservation
import com.app.miklink.e2e.support.ProcessFailureCollector
import com.app.miklink.e2e.support.ScenarioSession
import com.app.miklink.e2e.support.ScenarioStepResult
import com.app.miklink.e2e.support.StepKind
import com.app.miklink.e2e.support.StepStatus
import com.app.miklink.e2e.support.TerminalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FailureEvidenceScenarioTest {
    @Test
    fun crashAndObservableAnrAreRetainedAsCurrentSessionEvidence() {
        val observations = listOf(
            ProcessExitObservation(99, ProcessExitKind.CRASH, "old crash"),
            ProcessExitObservation(101, ProcessExitKind.CRASH, "java crash"),
            ProcessExitObservation(102, ProcessExitKind.ANR, "input dispatch timeout"),
            ProcessExitObservation(103, ProcessExitKind.USER_REQUESTED, "force-stop")
        )
        val relevant = ProcessFailureCollector.relevantExits(observations, 100)
        assertEquals(listOf(ProcessExitKind.CRASH, ProcessExitKind.ANR), relevant.map { it.kind })

        val result = runningSession("runtime-failure").finish(
            status = TerminalStatus.FAIL,
            reasonCode = "PROCESS_FAILURE",
            endedAt = "2026-08-09T20:00:05Z",
            cleanup = CleanupResult(CleanupStatus.PASS),
            crashExitReason = relevant.joinToString { it.kind.name },
            artifactPaths = listOf("scenarios/runtime-failure/screenshot.png", "scenarios/runtime-failure/logcat.txt")
        )
        assertEquals("step-visible", result.lastSuccessfulStepId)
        assertTrue(result.crashExitReason!!.contains("CRASH"))
        assertTrue(result.crashExitReason!!.contains("ANR"))
        assertTrue(result.artifactPaths.any { it.endsWith("screenshot.png") })
    }

    @Test
    fun timeoutHasTerminalReasonLastStepAndObservableArtifacts() {
        val result = runningSession("timeout").finish(
            status = TerminalStatus.FAIL,
            reasonCode = "SCENARIO_TIMEOUT",
            endedAt = "2026-08-09T20:01:30Z",
            cleanup = CleanupResult(CleanupStatus.PASS),
            artifactPaths = listOf("scenarios/timeout/screenshot.png", "scenarios/timeout/ui-hierarchy.xml")
        )
        assertEquals(TerminalStatus.FAIL, result.status)
        assertEquals("SCENARIO_TIMEOUT", result.reasonCode)
        assertEquals("step-visible", result.lastSuccessfulStepId)
        assertEquals(2, result.artifactPaths.size)
    }

    @Test
    fun lostDeviceDoesNotClaimAnUnavailableScreenshot() {
        val result = runningSession("lost-device").finish(
            status = TerminalStatus.FAIL,
            reasonCode = "DEVICE_LOST",
            endedAt = "2026-08-09T20:02:00Z",
            cleanup = CleanupResult(CleanupStatus.NOT_REQUIRED),
            detail = "Device became unobservable after the last successful step"
        )
        assertEquals(TerminalStatus.FAIL, result.status)
        assertTrue(result.artifactPaths.isEmpty())
        assertNull(result.crashExitReason)
        assertEquals("step-visible", result.lastSuccessfulStepId)
    }

    @Test
    fun cleanupFailureOverridesAnOtherwisePassingScenario() {
        val result = runningSession("cleanup-failure").finish(
            status = TerminalStatus.PASS,
            reasonCode = "ASSERTIONS_PASSED",
            endedAt = "2026-08-09T20:03:00Z",
            cleanup = CleanupResult(CleanupStatus.FAIL, "FIXTURE_CLEANUP_FAILED")
        )
        assertEquals(TerminalStatus.FAIL, result.status)
        assertEquals("FIXTURE_CLEANUP_FAILED", result.reasonCode)
        assertEquals(CleanupStatus.FAIL, result.cleanup.status)
    }

    private fun runningSession(scenarioId: String): ScenarioSession =
        ScenarioSession("failure-evidence-session", scenarioId, "2026-08-09T20:00:00Z").apply {
            recordPrerequisite(
                PrerequisiteResult("device", true, PrerequisiteStatus.AVAILABLE, "DEVICE_CONNECTED")
            )
            markReady()
            startRunning()
            recordStep(
                ScenarioStepResult(
                    stepId = "step-visible",
                    kind = StepKind.OBSERVATION,
                    status = StepStatus.PASS,
                    startedAt = "2026-08-09T20:00:01Z",
                    endedAt = "2026-08-09T20:00:02Z"
                )
            )
        }
}

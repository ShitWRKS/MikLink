package com.app.miklink.e2e.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class TestSessionStateTest {
    @Test
    fun recordsLastSuccessfulStepAndTerminalResult() {
        val session = ScenarioSession("session", "scenario", "2026-08-09T10:00:00Z")
        session.markReady()
        session.startRunning()
        session.recordStep(step("setup", StepKind.SETUP, StepStatus.PASS))
        session.recordStep(step("assert", StepKind.ASSERTION, StepStatus.FAIL))

        val result = session.finish(
            status = TerminalStatus.FAIL,
            reasonCode = "ASSERTION_FAILED",
            endedAt = "2026-08-09T10:00:02Z",
            cleanup = CleanupResult(CleanupStatus.PASS)
        )

        assertEquals("setup", result.lastSuccessfulStepId)
        assertEquals(TerminalStatus.FAIL, result.status)
    }

    @Test
    fun unavailableRequiredPrerequisiteEndsNotRunBeforeExecution() {
        val session = ScenarioSession("session", "scenario", "2026-08-09T10:00:00Z")
        session.recordPrerequisite(
            PrerequisiteResult("probe", true, PrerequisiteStatus.UNAVAILABLE, "PROBE_MISSING")
        )

        val result = session.finishNotRun("PROBE_MISSING", "2026-08-09T10:00:01Z")

        assertEquals(TerminalStatus.NOT_RUN, result.status)
        assertNull(result.lastSuccessfulStepId)
    }

    @Test
    fun cannotReturnToRunningAfterTerminalState() {
        val session = ScenarioSession("session", "scenario", "2026-08-09T10:00:00Z")
        session.finishNotRun("DEVICE_MISSING", "2026-08-09T10:00:01Z")

        try {
            session.startRunning()
            fail("Expected terminal state to reject execution")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }

    private fun step(id: String, kind: StepKind, status: StepStatus) = ScenarioStepResult(
        stepId = id,
        kind = kind,
        status = status,
        startedAt = "2026-08-09T10:00:00Z",
        endedAt = "2026-08-09T10:00:01Z"
    )
}

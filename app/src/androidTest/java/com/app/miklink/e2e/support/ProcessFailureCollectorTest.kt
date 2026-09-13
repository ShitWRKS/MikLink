package com.app.miklink.e2e.support

import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessFailureCollectorTest {
    @Test
    fun keepsOnlyCrashAndAnrExitsFromCurrentSession() {
        val observations = listOf(
            ProcessExitObservation(100, ProcessExitKind.CRASH, "old"),
            ProcessExitObservation(201, ProcessExitKind.USER_REQUESTED, "force stop"),
            ProcessExitObservation(202, ProcessExitKind.ANR, "input dispatch")
        )

        val filtered = ProcessFailureCollector.relevantExits(observations, sessionStartedAtMs = 200)

        assertEquals(listOf(ProcessExitKind.ANR), filtered.map { it.kind })
    }
}

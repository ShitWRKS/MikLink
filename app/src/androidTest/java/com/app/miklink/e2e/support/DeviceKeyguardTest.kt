package com.app.miklink.e2e.support

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceKeyguardTest {
    @Test
    fun alreadyUnlockedContinuesImmediately() {
        var sleeps = 0
        assertTrue(
            waitForUnlockState(1_000, 100, { false }, { 0L }) { sleeps++ }
        )
        assertTrue(sleeps == 0)
    }

    @Test
    fun manualUnlockResumesSameWait() {
        var now = 0L
        var locked = true
        assertTrue(
            waitForUnlockState(
                timeoutMs = 1_000,
                pollIntervalMs = 100,
                isLocked = { locked },
                nowMs = { now },
                sleep = {
                    now += it
                    if (now >= 300L) locked = false
                }
            )
        )
    }

    @Test
    fun unresolvedLockStopsAtBound() {
        var now = 0L
        assertFalse(
            waitForUnlockState(500, 100, { true }, { now }) { now += it }
        )
        assertTrue(now >= 500L)
    }
}

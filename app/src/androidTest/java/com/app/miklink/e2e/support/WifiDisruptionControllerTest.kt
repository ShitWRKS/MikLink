package com.app.miklink.e2e.support

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WifiDisruptionControllerTest {
    @Test
    fun noOptInDoesNotReadOrMutateWifi() = runBlocking {
        val adapter = FakeWifiAdapter()
        val result = WifiDisruptionController(adapter).execute(SessionPolicy()) { error("must not run") }
        assertEquals(TerminalStatus.NOT_RUN, result.status)
        assertEquals("WIFI_DISRUPTION_OPT_IN_REQUIRED", result.reasonCode)
        assertEquals(0, adapter.captureCount)
        assertTrue(adapter.transitions.isEmpty())
    }

    @Test
    fun missingHostControlDoesNotMutateWifi() = runBlocking {
        val adapter = FakeWifiAdapter(hostAnswers = ArrayDeque(listOf(false)))
        val result = WifiDisruptionController(adapter).execute(authorizedPolicy()) { error("must not run") }
        assertEquals(TerminalStatus.NOT_RUN, result.status)
        assertEquals("HOST_CONTROL_NOT_RETAINED", result.reasonCode)
        assertTrue(adapter.transitions.isEmpty())
    }

    @Test
    fun actionFailureStillRestoresCapturedWifiState() = runBlocking {
        val adapter = FakeWifiAdapter()
        val result = WifiDisruptionController(adapter).execute(authorizedPolicy()) {
            throw IllegalStateException("EXPECTED_PROBE_LOSS")
        }
        assertEquals(TerminalStatus.FAIL, result.status)
        assertEquals(CleanupStatus.PASS, result.cleanup.status)
        assertEquals(listOf(false, true), adapter.transitions)
        assertTrue(adapter.enabled)
        assertTrue(result.actionExecuted)
    }

    @Test
    fun restorationFailureOverridesOtherwiseSuccessfulOutcome() = runBlocking {
        val adapter = FakeWifiAdapter(failEnable = true)
        val result = WifiDisruptionController(adapter).execute(authorizedPolicy()) { }
        assertEquals(TerminalStatus.FAIL, result.status)
        assertEquals("WIFI_RESTORE_FAILED", result.reasonCode)
        assertEquals(CleanupStatus.FAIL, result.cleanup.status)
        assertFalse(adapter.enabled)
    }

    @Test
    fun hostControlLostAfterDisableFailsAndRestoresWithoutRunningAction() = runBlocking {
        val adapter = FakeWifiAdapter(hostAnswers = ArrayDeque(listOf(true, false)))
        val result = WifiDisruptionController(adapter).execute(authorizedPolicy()) { error("must not run") }
        assertEquals(TerminalStatus.FAIL, result.status)
        assertEquals("HOST_CONTROL_LOST", result.reasonCode)
        assertFalse(result.actionExecuted)
        assertEquals(listOf(false, true), adapter.transitions)
    }

    private fun authorizedPolicy() = SessionPolicy(
        allowWifiDisruption = true,
        hostControlRetained = true
    )

    private class FakeWifiAdapter(
        var enabled: Boolean = true,
        private val hostAnswers: ArrayDeque<Boolean> = ArrayDeque(listOf(true, true)),
        private val failEnable: Boolean = false
    ) : WifiControlAdapter {
        var captureCount = 0
        val transitions = mutableListOf<Boolean>()

        override fun capture() = CapturedWifiState(enabled).also { captureCount++ }
        override fun hasRetainedHostControl(): Boolean = hostAnswers.removeFirstOrNull() ?: true
        override fun setEnabled(enabled: Boolean): Boolean {
            transitions += enabled
            if (enabled && failEnable) return false
            this.enabled = enabled
            return true
        }
        override fun awaitEnabled(enabled: Boolean, timeoutMs: Long): Boolean = this.enabled == enabled
    }
}

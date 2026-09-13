package com.app.miklink.e2e.support

import android.content.Context
import android.net.wifi.WifiManager
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

data class CapturedWifiState(val enabled: Boolean)

data class WifiDisruptionResult(
    val status: TerminalStatus,
    val reasonCode: String,
    val cleanup: CleanupResult,
    val actionExecuted: Boolean
)

interface WifiControlAdapter {
    fun capture(): CapturedWifiState
    fun hasRetainedHostControl(): Boolean
    fun setEnabled(enabled: Boolean): Boolean
    fun awaitEnabled(enabled: Boolean, timeoutMs: Long): Boolean
}

class WifiDisruptionController(
    private val adapter: WifiControlAdapter = AndroidWifiControlAdapter(),
    private val transitionTimeoutMs: Long = 15_000L
) {
    suspend fun execute(
        policy: SessionPolicy,
        actionWhileDisconnected: suspend () -> Unit
    ): WifiDisruptionResult {
        if (!policy.allowWifiDisruption) return notRun("WIFI_DISRUPTION_OPT_IN_REQUIRED")
        if (!policy.hostControlRetained || !adapter.hasRetainedHostControl()) {
            return notRun("HOST_CONTROL_NOT_RETAINED")
        }

        val initial = adapter.capture()
        if (!initial.enabled) return notRun("WIFI_NOT_ENABLED_AT_START")

        var attempted = false
        var actionExecuted = false
        var status = TerminalStatus.PASS
        var reason = "DISRUPTION_COMPLETED"
        var cleanup = CleanupResult(CleanupStatus.NOT_REQUIRED)
        try {
            attempted = true
            if (!adapter.setEnabled(false) || !adapter.awaitEnabled(false, transitionTimeoutMs)) {
                status = TerminalStatus.FAIL
                reason = "WIFI_DISABLE_FAILED"
            } else if (!adapter.hasRetainedHostControl()) {
                status = TerminalStatus.FAIL
                reason = "HOST_CONTROL_LOST"
            } else {
                actionExecuted = true
                try {
                    actionWhileDisconnected()
                } catch (failure: Throwable) {
                    status = TerminalStatus.FAIL
                    reason = failure.message?.takeIf { it.isNotBlank() } ?: "DISRUPTED_ACTION_FAILED"
                }
            }
        } finally {
            cleanup = if (attempted && initial.enabled &&
                adapter.setEnabled(true) && adapter.awaitEnabled(true, transitionTimeoutMs)
            ) {
                CleanupResult(CleanupStatus.PASS)
            } else {
                CleanupResult(CleanupStatus.FAIL, "WIFI_RESTORE_FAILED")
            }
        }
        if (cleanup.status == CleanupStatus.FAIL) {
            status = TerminalStatus.FAIL
            reason = cleanup.reasonCode ?: "WIFI_RESTORE_FAILED"
        }
        return WifiDisruptionResult(status, reason, cleanup, actionExecuted)
    }

    private fun notRun(reason: String) = WifiDisruptionResult(
        status = TerminalStatus.NOT_RUN,
        reasonCode = reason,
        cleanup = CleanupResult(CleanupStatus.NOT_REQUIRED),
        actionExecuted = false
    )
}

private class AndroidWifiControlAdapter : WifiControlAdapter {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val wifiManager: WifiManager by lazy {
        instrumentation.targetContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    override fun capture(): CapturedWifiState = CapturedWifiState(wifiManager.isWifiEnabled)

    override fun hasRetainedHostControl(): Boolean =
        shell("getprop sys.usb.state").split(',').any { it.trim() == "adb" }

    override fun setEnabled(enabled: Boolean): Boolean = runCatching {
        shell("svc wifi ${if (enabled) "enable" else "disable"}")
        true
    }.getOrDefault(false)

    override fun awaitEnabled(enabled: Boolean, timeoutMs: Long): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (wifiManager.isWifiEnabled == enabled) return true
            SystemClock.sleep(250)
        }
        return wifiManager.isWifiEnabled == enabled
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
            .also { descriptor.close() }
    }
}

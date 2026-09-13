package com.app.miklink.e2e.support

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/**
 * Requests the normal Android keyguard dismissal flow and waits for slower OEM
 * transitions. This does not disable the keyguard or bypass a PIN/password.
 */
fun UiDevice.dismissKeyguardIfPossible(
    context: Context,
    timeoutMs: Long = 60_000L,
    pollIntervalMs: Long = 250L
): Boolean {
    wakeUp()
    executeShellCommand("wm dismiss-keyguard")
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    if (keyguard.isDeviceLocked) {
        Log.w(TAG, "DEVICE_UNLOCK_REQUIRED: unlock the selected device to continue this operation")
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply {
                putString("miklink.preflight", "DEVICE_UNLOCK_REQUIRED")
                putLong("miklink.unlockTimeoutMs", timeoutMs)
            }
        )
    }
    return waitForUnlockState(
        timeoutMs = timeoutMs,
        pollIntervalMs = pollIntervalMs,
        isLocked = { keyguard.isDeviceLocked },
        nowMs = SystemClock::uptimeMillis,
        sleep = SystemClock::sleep
    )
}

internal fun waitForUnlockState(
    timeoutMs: Long,
    pollIntervalMs: Long,
    isLocked: () -> Boolean,
    nowMs: () -> Long,
    sleep: (Long) -> Unit
): Boolean {
    require(timeoutMs >= 0L)
    require(pollIntervalMs > 0L)
    val deadline = nowMs() + timeoutMs
    while (isLocked() && nowMs() < deadline) {
        sleep(pollIntervalMs.coerceAtMost((deadline - nowMs()).coerceAtLeast(1L)))
    }
    return !isLocked()
}

private const val TAG = "MikLinkDevicePreflight"

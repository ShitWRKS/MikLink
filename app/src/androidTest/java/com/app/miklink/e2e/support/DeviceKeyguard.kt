package com.app.miklink.e2e.support

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import androidx.test.uiautomator.UiDevice

/**
 * Requests the normal Android keyguard dismissal flow and waits for slower OEM
 * transitions. This does not disable the keyguard or bypass a PIN/password.
 */
fun UiDevice.dismissKeyguardIfPossible(
    context: Context,
    timeoutMs: Long = 10_000L,
    pollIntervalMs: Long = 100L
): Boolean {
    wakeUp()
    executeShellCommand("wm dismiss-keyguard")
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    while (keyguard.isDeviceLocked && SystemClock.uptimeMillis() < deadline) {
        SystemClock.sleep(pollIntervalMs)
    }
    return !keyguard.isDeviceLocked
}

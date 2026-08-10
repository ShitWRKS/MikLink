package com.app.miklink.e2e.support

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.app.miklink.core.domain.test.logging.LogSanitizer
import java.io.FileInputStream

enum class ProcessExitKind { CRASH, NATIVE_CRASH, ANR, USER_REQUESTED, OTHER }

data class ProcessExitObservation(
    val timestampMs: Long,
    val kind: ProcessExitKind,
    val description: String?
)

data class ProcessFailureEvidence(
    val appVisible: Boolean,
    val exits: List<ProcessExitObservation>,
    val logcatExcerpt: String?
)

class ProcessFailureCollector(
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext,
    private val sanitizer: LogSanitizer = LogSanitizer()
) {
    fun collect(sessionStartedAtMs: Long, sessionId: String): ProcessFailureEvidence {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val observations = activityManager
            .getHistoricalProcessExitReasons(context.packageName, 0, 20)
            .map { info ->
                ProcessExitObservation(
                    timestampMs = info.timestamp,
                    kind = info.reason.toKind(),
                    description = info.description?.let(sanitizer::sanitize)
                )
            }
        return ProcessFailureEvidence(
            appVisible = uiDevice.currentPackageName == context.packageName,
            exits = relevantExits(observations, sessionStartedAtMs),
            logcatExcerpt = readTargetedLogcat(sessionId)
        )
    }

    private fun readTargetedLogcat(sessionId: String): String? = runCatching {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("logcat -d -v threadtime *:W")
        FileInputStream(descriptor.fileDescriptor).bufferedReader().useLines { lines ->
            lines.filter { it.contains(context.packageName) || it.contains(sessionId) }
                .takeLastBounded(200)
                .joinToString("\n")
                .takeIf { it.isNotBlank() }
                ?.let(sanitizer::sanitize)
        }.also { descriptor.close() }
    }.getOrNull()

    private fun Sequence<String>.takeLastBounded(limit: Int): List<String> {
        val buffer = ArrayDeque<String>(limit)
        forEach { line ->
            if (buffer.size == limit) buffer.removeFirst()
            buffer.addLast(line)
        }
        return buffer.toList()
    }

    companion object {
        fun relevantExits(
            observations: List<ProcessExitObservation>,
            sessionStartedAtMs: Long
        ): List<ProcessExitObservation> = observations.filter {
            it.timestampMs >= sessionStartedAtMs &&
                it.kind in setOf(ProcessExitKind.CRASH, ProcessExitKind.NATIVE_CRASH, ProcessExitKind.ANR)
        }

        private fun Int.toKind(): ProcessExitKind = when (this) {
            ApplicationExitInfo.REASON_CRASH -> ProcessExitKind.CRASH
            ApplicationExitInfo.REASON_CRASH_NATIVE -> ProcessExitKind.NATIVE_CRASH
            ApplicationExitInfo.REASON_ANR -> ProcessExitKind.ANR
            ApplicationExitInfo.REASON_USER_REQUESTED -> ProcessExitKind.USER_REQUESTED
            else -> ProcessExitKind.OTHER
        }
    }
}

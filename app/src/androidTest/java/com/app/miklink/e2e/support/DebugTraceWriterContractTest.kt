package com.app.miklink.e2e.support

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.miklink.core.domain.test.logging.DebugTraceSinkImpl
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugTraceWriterContractTest {
    @Test
    fun rejectsBlankRunSourceAndUnknownEventBeforePersistence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val traceDirectory = File(context.getExternalFilesDir(null), "e2e-trace")
        val existing = traceDirectory.listFiles().orEmpty().mapTo(mutableSetOf()) { it.canonicalPath }
        val sink = DebugTraceSinkImpl(context)

        assertThrows(IllegalArgumentException::class.java) {
            sink.startRun("", mapOf("sessionId" to "session", "scenarioId" to "scenario"))
        }

        val runId = sink.startRun(
            "contract-test",
            mapOf("sessionId" to "session", "scenarioId" to "scenario")
        )
        try {
            assertThrows(IllegalArgumentException::class.java) {
                sink.event(runId, "unknown_event_type")
            }
        } finally {
            sink.finishRun(runId, "PASS")
            traceDirectory.listFiles().orEmpty()
                .filterNot { it.canonicalPath in existing }
                .forEach(File::delete)
        }
    }
}

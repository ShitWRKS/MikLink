package com.app.miklink.e2e.catalog

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestProgressKey
import com.app.miklink.core.domain.test.model.TestRunSnapshot
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCase
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.ui.test.TestViewModel
import com.app.miklink.utils.UiState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RapidStartScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("rapid-start")

    @Test
    fun newestStartOwnsStateAndEveryRunTerminatesOrIsCancelled() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val firstStarted = CountDownLatch(1)
        val firstCancelled = CountDownLatch(1)
        val invocation = AtomicInteger(0)
        val finalOutcome = TestOutcome(
            overallStatus = "PASS",
            finalSnapshot = TestRunSnapshot(progress = TestProgressKey.COMPLETED, percent = 100),
            rawResultsJson = "{\"owner\":\"second\"}"
        )
        val runUseCase = object : RunTestUseCase {
            override fun execute(plan: TestPlan): Flow<TestEvent> = when (invocation.incrementAndGet()) {
                1 -> flow {
                    firstStarted.countDown()
                    try {
                        awaitCancellation()
                    } finally {
                        firstCancelled.countDown()
                    }
                }
                else -> flowOf(TestEvent.Completed(finalOutcome))
            }
        }
        val saveUseCase = object : SaveTestReportUseCase {
            override suspend fun invoke(report: TestReport, incrementClientCounter: Boolean): Long = 1L
        }
        lateinit var viewModel: TestViewModel
        instrumentation.runOnMainSync {
            viewModel = TestViewModel(
                instrumentation.targetContext.applicationContext as Context,
                SavedStateHandle(mapOf("clientId" to 1L, "profileId" to 1L, "socketName" to "E2E")),
                runUseCase,
                saveUseCase
            )
            viewModel.startTest()
        }
        assertTrue("First run did not start", firstStarted.await(5, TimeUnit.SECONDS))

        instrumentation.runOnMainSync { viewModel.startTest() }
        assertTrue("Replaced run did not reach cancellation", firstCancelled.await(5, TimeUnit.SECONDS))
        waitUntil(5_000) { viewModel.uiState.value is UiState.Success && !viewModel.isRunning.value }

        val success = viewModel.uiState.value as UiState.Success
        assertEquals("{\"owner\":\"second\"}", success.data.resultsJson)
        assertEquals(2, invocation.get())
        assertFalse(viewModel.isRunning.value)
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(25)
        assertTrue("Expected terminal state was not reached", condition())
    }
}

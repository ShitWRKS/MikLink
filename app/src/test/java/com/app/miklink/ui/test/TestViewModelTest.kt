/*
 * Purpose: Validate TestViewModel maps domain events to UI state flows for snapshots, reports, and logs.
 * Inputs: Synthetic TestEvent streams emitted via a fake RunTestUseCase and SavedStateHandle navigation args.
 * Outputs: Assertions on snapshot progression, UiState transitions, and log accumulation.
 */
package com.app.miklink.ui.test

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestProgress
import com.app.miklink.core.domain.test.model.TestProgressKey
import com.app.miklink.core.domain.test.model.TestRunSnapshot
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.core.domain.test.model.TestSectionStatus
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCase
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.testsupport.MainDispatcherRule
import com.app.miklink.utils.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import io.mockk.every
import io.mockk.mockk

// ---------------------------------------------------------------------------
// Test helpers
// ---------------------------------------------------------------------------

/**
 * Controllable [RunTestUseCase] that creates a fresh [RunInvocation] per [execute] call.
 */
private class ControllableRunTestUseCase : RunTestUseCase {
    val invocations = mutableListOf<RunInvocation>()

    override fun execute(plan: TestPlan): Flow<TestEvent> {
        val invocation = RunInvocation(plan)
        invocations += invocation
        return invocation.asFlow()
    }
}

/**
 * Controls a single test run via a [Channel]-backed [Flow].
 *
 * Signals:
 * - [started]: completes when the flow is first collected.
 * - [cancelled]: records whether the coroutine was cancelled.
 * - [finallyEntered]: completes when the `finally` block begins.
 * - [allowFinallyToComplete]: complete this to let the `finally` block finish.
 *
 * Emits:
 * - [emit] sends events to the collector.
 */
private class RunInvocation(val plan: TestPlan) {
    private val channel = Channel<TestEvent>(Channel.UNLIMITED)

    val started = CompletableDeferred<Unit>()

    @Volatile
    var cancelled: Boolean = false
        private set

    val finallyEntered = CompletableDeferred<Unit>()
    val allowFinallyToComplete = CompletableDeferred<Unit>()

    suspend fun emit(event: TestEvent) {
        channel.send(event)
    }

    /** Close the channel so the `for` loop exits naturally. */
    fun close() {
        channel.close()
    }

    /** Cleanup: close channel and release barrier. */
    fun cleanup() {
        channel.close()
        allowFinallyToComplete.complete(Unit)
    }

    fun asFlow(): Flow<TestEvent> = flow {
        started.complete(Unit)
        val job = currentCoroutineContext()[Job]!!
        try {
            for (event in channel) {
                emit(event)
            }
        } finally {
            withContext(NonCancellable) {
                if (job.isCancelled) {
                    cancelled = true
                }
                finallyEntered.complete(Unit)
                allowFinallyToComplete.await()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TestViewModelTest
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val mainDispatcher = StandardTestDispatcher(testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    /** Runs a test with an explicit shared scheduler so all dispatchers see the same clock. */
    private fun runViewModelTest(
        testBody: suspend TestScope.() -> Unit
    ): TestResult = runTest(
        context = StandardTestDispatcher(testScheduler),
        testBody = testBody
    )

    private val reportRepository = object : SaveTestReportUseCase {
        private val state = kotlinx.coroutines.flow.MutableStateFlow<List<TestReport>>(emptyList())

        override suspend fun invoke(report: TestReport, incrementClientCounter: Boolean): Long {
            val id = (state.value.maxOfOrNull { it.reportId } ?: 0L) + 1L
            val r = report.copy(reportId = id)
            state.value = state.value + r
            return id
        }
    }

    private fun createViewModel(
        useCase: RunTestUseCase,
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            mapOf("clientId" to 1L, "profileId" to 1L, "socketName" to "A1")
        )
    ): TestViewModel {
        val mockContext = mockk<Context>(relaxed = true) {
            every { getString(any(), any<Int>()) } returns "Test timeout"
        }
        return TestViewModel(mockContext, savedStateHandle, useCase, reportRepository)
    }

    // Shared fixtures
    private val defaultSnapshot = TestRunSnapshot(
        sections = listOf(
            TestSectionSnapshot(id = TestSectionId.NETWORK, status = TestSectionStatus.PENDING),
            TestSectionSnapshot(id = TestSectionId.LINK, status = TestSectionStatus.PENDING)
        ),
        progress = TestProgressKey.PREPARING,
        percent = 0
    )

    private val defaultOutcome = TestOutcome(
        overallStatus = "PASS",
        finalSnapshot = defaultSnapshot.copy(percent = 100),
        rawResultsJson = "{}"
    )

    private companion object {
        const val TEST_TIMEOUT_MS = 90_000L
    }

    // =======================================================================
    // Existing tests
    // =======================================================================

    @Test
    fun `snapshot state updates progressively from domain events`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        viewModel.startTest()
        runCurrent()

        val inv = useCase.invocations.first()
        assertTrue("Run should be started", inv.started.isCompleted)

        val pendingSnapshot = TestRunSnapshot(
            sections = listOf(
                TestSectionSnapshot(id = TestSectionId.NETWORK, status = TestSectionStatus.PENDING),
                TestSectionSnapshot(id = TestSectionId.LINK, status = TestSectionStatus.PENDING)
            ),
            progress = TestProgressKey.PREPARING,
            percent = 0
        )
        inv.emit(TestEvent.SnapshotUpdated(pendingSnapshot))
        runCurrent()

        assertEquals(2, viewModel.snapshot.value?.sections?.size)
        assertTrue(viewModel.snapshot.value?.sections?.all { it.status == TestSectionStatus.PENDING } == true)

        val runningSnapshot = pendingSnapshot.copy(
            sections = listOf(
                TestSectionSnapshot(id = TestSectionId.NETWORK, status = TestSectionStatus.RUNNING),
                TestSectionSnapshot(id = TestSectionId.LINK, status = TestSectionStatus.PENDING)
            ),
            progress = TestProgressKey.NETWORK_CONFIG,
            percent = 20
        )
        inv.emit(TestEvent.SnapshotUpdated(runningSnapshot))
        runCurrent()

        val currentStatuses = viewModel.snapshot.value?.sections?.associate { it.id.name to it.status }
        assertEquals(TestSectionStatus.RUNNING, currentStatuses?.get("NETWORK"))
        assertEquals(TestSectionStatus.PENDING, currentStatuses?.get("LINK"))

        val finalSnapshot = runningSnapshot.copy(
            sections = listOf(
                TestSectionSnapshot(id = TestSectionId.NETWORK, status = TestSectionStatus.PASS),
                TestSectionSnapshot(id = TestSectionId.LINK, status = TestSectionStatus.FAIL, warning = "link down")
            ),
            progress = TestProgressKey.COMPLETED,
            percent = 100
        )
        inv.emit(TestEvent.SnapshotUpdated(finalSnapshot))
        runCurrent()

        inv.emit(TestEvent.Completed(TestOutcome(overallStatus = "FAIL", finalSnapshot = finalSnapshot, rawResultsJson = "{}")))
        runCurrent()

        assertTrue(viewModel.uiState.value is UiState.Success<*>)
        assertEquals(listOf(TestSectionStatus.PASS, TestSectionStatus.FAIL), viewModel.snapshot.value?.sections?.map { it.status })

        inv.allowFinallyToComplete.complete(Unit)
        runCurrent()
    }

    @Test
    fun `logs are collected from progress and log line events`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        viewModel.startTest()
        runCurrent()

        val inv = useCase.invocations.first()
        inv.emit(TestEvent.Progress(TestProgress("Init", 0, "starting setup")))
        inv.emit(TestEvent.LogLine("Sanitized log line"))
        runCurrent()

        assertEquals(listOf("[Init] starting setup", "Sanitized log line"), viewModel.logs.value)

        inv.allowFinallyToComplete.complete(Unit)
        runCurrent()
    }

    @Test
    fun `cancellation does not surface a false failure ui state`() = runViewModelTest {
        val useCase = object : RunTestUseCase {
            override fun execute(plan: TestPlan): Flow<TestEvent> = flow {
                throw CancellationException("screen closed")
            }
        }

        val viewModel = createViewModel(useCase)

        viewModel.startTest()
        runCurrent()

        assertTrue(viewModel.uiState.value !is UiState.Error)
        assertFalse(viewModel.isRunning.value)
    }

    // =======================================================================
    // Test 1 — Second start cancels and waits for first run
    // =======================================================================

    @Test
    fun `second start cancels and waits for first run`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        // Start run A.
        viewModel.startTest()
        runCurrent()

        val first = useCase.invocations[0]
        assertTrue("First run should be started", first.started.isCompleted)

        // Start run B. cancelAndJoin must wait for A's finally.
        viewModel.startTest()
        runCurrent()

        // A's finally was entered (cancelled) but is blocked on the barrier.
        assertTrue("First run should be cancelled", first.cancelled)

        // B cannot have started yet because cancelAndJoin is waiting for A's finally.
        assertEquals("Only one invocation so far — B has not started", 1, useCase.invocations.size)

        // Release A's finally so cancelAndJoin completes.
        first.allowFinallyToComplete.complete(Unit)
        runCurrent()

        // Now B has started.
        assertEquals(2, useCase.invocations.size)
        assertTrue("Second run should be started", useCase.invocations[1].started.isCompleted)

        // Cleanup
        useCase.invocations[1].cleanup()
        runCurrent()
    }

    // =======================================================================
    // Test 2 — Old finally cannot reset new running state
    // =======================================================================

    @Test
    fun `old finally cannot reset new running state`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        // Start run A and emit a snapshot.
        viewModel.startTest()
        runCurrent()
        useCase.invocations[0].emit(TestEvent.SnapshotUpdated(defaultSnapshot))
        runCurrent()

        // Replace with run B.
        viewModel.startTest()
        runCurrent()

        val first = useCase.invocations[0]

        // A's finally was entered but blocked; release it.
        assertTrue("First should be cancelled", first.cancelled)
        first.allowFinallyToComplete.complete(Unit)
        runCurrent()

        // B should be active now.
        assertTrue("isRunning must be true while B is active", viewModel.isRunning.value)
        assertEquals(2, useCase.invocations.size)
        assertTrue("Second run should be started", useCase.invocations[1].started.isCompleted)

        // Cleanup
        useCase.invocations[1].cleanup()
        runCurrent()
    }

    // =======================================================================
    // Test 3 — Stale snapshot is ignored
    // =======================================================================

    @Test
    fun `stale snapshot is ignored`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        // Run A emits its snapshot.
        viewModel.startTest()
        runCurrent()
        useCase.invocations[0].emit(TestEvent.SnapshotUpdated(defaultSnapshot.copy(percent = 42)))
        runCurrent()
        assertEquals(42, viewModel.snapshot.value?.percent)

        // Replace with run B.
        viewModel.startTest()
        runCurrent()

        val first = useCase.invocations[0]
        first.allowFinallyToComplete.complete(Unit)
        runCurrent()

        val freshSnapshot = defaultSnapshot.copy(percent = 99, progress = TestProgressKey.COMPLETED)
        useCase.invocations[1].emit(TestEvent.SnapshotUpdated(freshSnapshot))
        runCurrent()

        assertEquals("B's snapshot must prevail", 99, viewModel.snapshot.value?.percent)
        assertEquals(TestProgressKey.COMPLETED, viewModel.snapshot.value?.progress)

        // Cleanup
        useCase.invocations[1].allowFinallyToComplete.complete(Unit)
        runCurrent()
    }

    // =======================================================================
    // Test 4 — Stale completed event is ignored
    // =======================================================================

    @Test
    fun `stale completed event is ignored`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        // Run A emits snapshot then Completed.
        viewModel.startTest()
        runCurrent()
        useCase.invocations[0].emit(TestEvent.SnapshotUpdated(defaultSnapshot))
        useCase.invocations[0].emit(TestEvent.Completed(defaultOutcome))
        runCurrent()
        assertTrue(viewModel.uiState.value is UiState.Success<*>)

        // Replace with run B.
        viewModel.startTest()
        runCurrent()

        val first = useCase.invocations[0]
        first.allowFinallyToComplete.complete(Unit)
        runCurrent()

        // Run B completes with its own outcome.
        val outcomeB = TestOutcome(
            overallStatus = "FAIL",
            finalSnapshot = defaultSnapshot.copy(percent = 55),
            rawResultsJson = "{\"v\":2}"
        )
        useCase.invocations[1].emit(TestEvent.SnapshotUpdated(outcomeB.finalSnapshot))
        useCase.invocations[1].emit(TestEvent.Completed(outcomeB))
        runCurrent()

        assertTrue(viewModel.uiState.value is UiState.Success<*>)
        val report = (viewModel.uiState.value as UiState.Success<TestReport>).data
        assertEquals("Report must come from B", "{\"v\":2}", report.resultsJson)
        assertEquals("Snapshot must come from B", 55, viewModel.snapshot.value?.percent)

        // Cleanup
        useCase.invocations[1].allowFinallyToComplete.complete(Unit)
        runCurrent()
    }

    // =======================================================================
    // Test 5 — Stale failure is ignored
    // =======================================================================

    @Test
    fun `stale failure is ignored`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        // Run A is active.
        viewModel.startTest()
        runCurrent()
        useCase.invocations[0].emit(TestEvent.SnapshotUpdated(defaultSnapshot.copy(percent = 30)))
        runCurrent()

        // Replace with run B.
        viewModel.startTest()
        runCurrent()

        val first = useCase.invocations[0]
        first.allowFinallyToComplete.complete(Unit)
        runCurrent()

        // B emits a snapshot then fails.
        useCase.invocations[1].emit(TestEvent.SnapshotUpdated(defaultSnapshot.copy(percent = 70)))
        useCase.invocations[1].emit(TestEvent.Failed(TestError.Unexpected("B error")))
        runCurrent()

        assertTrue(viewModel.uiState.value is UiState.Error)
        assertEquals("B error", (viewModel.uiState.value as UiState.Error).message)
        assertEquals(70, viewModel.snapshot.value?.percent)

        // Cleanup
        useCase.invocations[1].allowFinallyToComplete.complete(Unit)
        runCurrent()
    }

    // =======================================================================
    // Test 6 — Stale logs are ignored
    // =======================================================================

    @Test
    fun `stale logs are ignored`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        // Run A emits a log.
        viewModel.startTest()
        runCurrent()
        useCase.invocations[0].emit(TestEvent.LogLine("A-log-1"))
        runCurrent()
        assertEquals(listOf("A-log-1"), viewModel.logs.value)

        // Replace with run B. Logs are cleared when new run starts.
        viewModel.startTest()
        runCurrent()

        val first = useCase.invocations[0]
        first.allowFinallyToComplete.complete(Unit)
        runCurrent()

        assertTrue("Logs must be cleared for new run", viewModel.logs.value.isEmpty())

        // B emits its own log.
        useCase.invocations[1].emit(TestEvent.LogLine("B-log-1"))
        runCurrent()

        assertEquals("Only B's log must be visible", listOf("B-log-1"), viewModel.logs.value)

        // Cleanup
        useCase.invocations[1].allowFinallyToComplete.complete(Unit)
        runCurrent()
    }

    // =======================================================================
    // Test 7 — Invalid second plan does not cancel current run
    // =======================================================================

    @Test
    fun `invalid second plan does not cancel current run`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val savedStateHandle = SavedStateHandle(
            mapOf("clientId" to 1L, "profileId" to 1L, "socketName" to "A1")
        )
        val viewModel = createViewModel(useCase, savedStateHandle)

        // Start run A and emit a snapshot + log.
        viewModel.startTest()
        runCurrent()
        useCase.invocations[0].emit(TestEvent.SnapshotUpdated(defaultSnapshot))
        useCase.invocations[0].emit(TestEvent.LogLine("A-log"))
        runCurrent()

        assertEquals(1, useCase.invocations.size)
        assertTrue(viewModel.isRunning.value)
        assertEquals(defaultSnapshot, viewModel.snapshot.value)
        assertEquals(listOf("A-log"), viewModel.logs.value)

        // Mutate SavedStateHandle so the next buildPlan() is invalid.
        savedStateHandle["clientId"] = -1L

        viewModel.startTest()
        runCurrent()

        assertEquals("No new run should start", 1, useCase.invocations.size)
        assertTrue("isRunning must stay true", viewModel.isRunning.value)
        assertEquals("Snapshot must not be zeroed", defaultSnapshot, viewModel.snapshot.value)
        assertEquals("Logs must not be zeroed", listOf("A-log"), viewModel.logs.value)
        assertTrue(
            "uiState must be error from invalid plan",
            viewModel.uiState.value is UiState.Error
        )

        // Cleanup
        useCase.invocations[0].allowFinallyToComplete.complete(Unit)
        runCurrent()
    }

    // =======================================================================
    // Test 8 — Cancellation does not surface an error
    // =======================================================================

    @Test
    fun `cancellation does not surface an error ui state on replacement`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        // Start run A.
        viewModel.startTest()
        runCurrent()

        // Replace with run B — A is cancelled, not failed.
        viewModel.startTest()
        runCurrent()

        val first = useCase.invocations[0]
        first.allowFinallyToComplete.complete(Unit)
        runCurrent()

        assertTrue(
            "No Error state from cancelled run A",
            viewModel.uiState.value !is UiState.Error
        )
        assertTrue("B should be running", viewModel.isRunning.value)

        // Cleanup
        useCase.invocations[1].allowFinallyToComplete.complete(Unit)
        runCurrent()
    }

    // =======================================================================
    // Test 9 — Current run completion resets running
    // =======================================================================

    @Test
    fun `current run completion resets running`() = runViewModelTest {
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        viewModel.startTest()
        runCurrent()
        assertTrue(viewModel.isRunning.value)

        // Complete the current run normally. Close channel so the for loop exits.
        useCase.invocations[0].emit(TestEvent.SnapshotUpdated(defaultSnapshot))
        useCase.invocations[0].emit(TestEvent.Completed(defaultOutcome))
        useCase.invocations[0].close()
        runCurrent()

        // The flow's finally entered and is blocked on the barrier. Release it.
        useCase.invocations[0].allowFinallyToComplete.complete(Unit)
        runCurrent()

        assertFalse("isRunning must be false after completion", viewModel.isRunning.value)
        assertTrue(viewModel.uiState.value is UiState.Success<*>)
        assertEquals(
            defaultOutcome.overallStatus,
            (viewModel.uiState.value as UiState.Success<TestReport>).data.overallStatus
        )
    }

    // =======================================================================
    // Test 10 — Current run timeout resets running
    // =======================================================================

    @Test
    fun `current run timeout resets running`() = runViewModelTest {
        // Use ControllableRunTestUseCase so the flow hangs on the channel.
        // The 90-second timeout will fire.
        val useCase = ControllableRunTestUseCase()
        val viewModel = createViewModel(useCase)

        viewModel.startTest()
        runCurrent()

        val inv = useCase.invocations[0]
        assertTrue(inv.started.isCompleted)
        assertTrue(viewModel.isRunning.value)
        assertNull(viewModel.snapshot.value)

        // Advance past the 90-second timeout.
        advanceTimeBy(TEST_TIMEOUT_MS + 1)
        runCurrent()

        // The timeout fired. The flow's finally entered but is blocked on the barrier.
        // Release the barrier so the flow can finish.
        inv.cleanup()
        runCurrent()

        assertTrue("uiState must be Error on timeout", viewModel.uiState.value is UiState.Error)
        assertEquals("Test timeout", (viewModel.uiState.value as UiState.Error).message)
        assertFalse("isRunning must be false after timeout", viewModel.isRunning.value)
    }
}

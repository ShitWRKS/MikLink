/*
 * Purpose: Validate TestViewModel maps domain events to UI state flows for snapshots, reports, and logs.
 * Inputs: Synthetic TestEvent streams emitted via a fake RunTestUseCase and SavedStateHandle navigation args.
 * Outputs: Assertions on snapshot progression, UiState transitions, and log accumulation.
 */
package com.app.miklink.ui.test

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestProgress
import com.app.miklink.core.domain.test.model.TestProgressKey
import com.app.miklink.core.domain.test.model.TestRunSnapshot
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.core.domain.test.model.TestSectionStatus
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCase
import com.app.miklink.testsupport.MainDispatcherRule
import com.app.miklink.utils.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import io.mockk.every
import io.mockk.mockk

class TestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reportRepository = object : SaveTestReportUseCase {
        private val state = kotlinx.coroutines.flow.MutableStateFlow<List<TestReport>>(emptyList())

        override suspend fun invoke(report: TestReport, incrementClientCounter: Boolean): Long {
            val id = (state.value.maxOfOrNull { it.reportId } ?: 0L) + 1L
            val r = report.copy(reportId = id)
            state.value = state.value + r
            return id
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `snapshot state updates progressively from domain events`() = runTest {
        val events = MutableSharedFlow<TestEvent>()
        val useCase = object : RunTestUseCase {
            override fun execute(plan: TestPlan): Flow<TestEvent> = events
        }

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "clientId" to 1L,
                "profileId" to 1L,
                "socketName" to "A1"
            )
        )

        val mockContext = mockk<Context>(relaxed = true) {
            every { getString(any(), any<Int>()) } returns "Test timeout"
        }

        val viewModel = TestViewModel(mockContext, savedStateHandle, useCase, reportRepository)

        viewModel.startTest()
        runCurrent() // Start the coroutine and reach the collect suspension point
        yield() // Ensure the collector is ready to receive events

        val pendingSnapshot = TestRunSnapshot(
            sections = listOf(
                TestSectionSnapshot(id = TestSectionId.NETWORK, status = TestSectionStatus.PENDING),
                TestSectionSnapshot(id = TestSectionId.LINK, status = TestSectionStatus.PENDING)
            ),
            progress = TestProgressKey.PREPARING,
            percent = 0
        )
        events.emit(TestEvent.SnapshotUpdated(pendingSnapshot))
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
        events.emit(TestEvent.SnapshotUpdated(runningSnapshot))
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
        events.emit(TestEvent.SnapshotUpdated(finalSnapshot))
        runCurrent()

        events.emit(
            TestEvent.Completed(
                TestOutcome(
                    overallStatus = "FAIL",
                    finalSnapshot = finalSnapshot,
                    rawResultsJson = "{}"
                )
            )
        )
        runCurrent()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is UiState.Success<TestReport>)
        val sectionsStatuses = viewModel.snapshot.value?.sections?.map { it.status }
        assertEquals(listOf(TestSectionStatus.PASS, TestSectionStatus.FAIL), sectionsStatuses)

        viewModel.viewModelScope.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `logs are collected from progress and log line events`() = runTest {
        val events = MutableSharedFlow<TestEvent>()
        val useCase = object : RunTestUseCase {
            override fun execute(plan: TestPlan): Flow<TestEvent> = events
        }

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "clientId" to 1L,
                "profileId" to 1L,
                "socketName" to "A1"
            )
        )

        val mockContext = mockk<Context>(relaxed = true) {
            every { getString(any(), any<Int>()) } returns "Test timeout"
        }

        val viewModel = TestViewModel(mockContext, savedStateHandle, useCase, reportRepository)

        viewModel.startTest()
        runCurrent() // Start the coroutine and reach the collect suspension point
        yield() // Ensure the collector is ready to receive events

        events.emit(TestEvent.Progress(TestProgress("Init", 0, "starting setup")))
        events.emit(TestEvent.LogLine("Sanitized log line"))
        runCurrent()

        assertEquals(listOf("[Init] starting setup", "Sanitized log line"), viewModel.logs.value)

        viewModel.viewModelScope.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancellation does not surface a false failure ui state`() = runTest {
        val useCase = object : RunTestUseCase {
            override fun execute(plan: TestPlan): Flow<TestEvent> = flow {
                throw CancellationException("screen closed")
            }
        }

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "clientId" to 1L,
                "profileId" to 1L,
                "socketName" to "A1"
            )
        )

        val mockContext = mockk<Context>(relaxed = true) {
            every { getString(any(), any<Int>()) } returns "Test timeout"
        }

        val viewModel = TestViewModel(mockContext, savedStateHandle, useCase, reportRepository)

        viewModel.startTest()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value !is UiState.Error)
        assertEquals(false, viewModel.isRunning.value)

        viewModel.viewModelScope.cancel()
    }
}

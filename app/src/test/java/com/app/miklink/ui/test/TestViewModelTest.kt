package com.app.miklink.ui.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestSectionResult
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.testsupport.MainDispatcherRule
import com.app.miklink.utils.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reportRepository = object : ReportRepository {
        private val state = kotlinx.coroutines.flow.MutableStateFlow<List<TestReport>>(emptyList())

        override suspend fun saveReport(report: TestReport): Long {
            val id = (state.value.maxOfOrNull { it.reportId } ?: 0L) + 1L
            val r = report.copy(reportId = id)
            state.value = state.value + r
            return id
        }

        override suspend fun getReport(id: Long): TestReport? = state.value.firstOrNull { it.reportId == id }

        override fun observeAllReports(): Flow<List<TestReport>> = state

        override fun observeReportsByClient(clientId: Long): Flow<List<TestReport>> =
            state.map { list -> list.filter { it.clientId == clientId } }

        override suspend fun deleteReport(report: TestReport) {
            state.value = state.value.filterNot { it.reportId == report.reportId }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sections state updates progressively from domain events`() = runTest {
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

        val viewModel = TestViewModel(savedStateHandle, useCase, reportRepository)

        viewModel.startTest()
        advanceUntilIdle()

        val pendingSections = listOf(
            TestSectionResult(type = "NETWORK", title = "Network", status = "PENDING"),
            TestSectionResult(type = "LINK", title = "Link", status = "PENDING")
        )
        events.emit(TestEvent.SectionsUpdated(pendingSections))
        advanceUntilIdle()

        assertEquals(2, viewModel.sections.value.size)
        assertTrue(viewModel.sections.value.all { it.status == "PENDING" })

        val runningSections = listOf(
            TestSectionResult(type = "NETWORK", title = "Network", status = "RUNNING"),
            TestSectionResult(type = "LINK", title = "Link", status = "PENDING")
        )
        events.emit(TestEvent.SectionsUpdated(runningSections))
        advanceUntilIdle()

        val currentStatuses = viewModel.sections.value.associate { it.type.name to it.status }
        assertEquals("RUNNING", currentStatuses["NETWORK"])
        assertEquals("PENDING", currentStatuses["LINK"])

        val finalSections = listOf(
            TestSectionResult(type = "NETWORK", title = "Network", status = "PASS"),
            TestSectionResult(type = "LINK", title = "Link", status = "FAIL", details = mapOf("error" to "link down"))
        )
        events.emit(TestEvent.SectionsUpdated(finalSections))
        advanceUntilIdle()

        events.emit(
            TestEvent.Completed(
                TestOutcome(
                    overallStatus = "FAIL",
                    sections = finalSections,
                    rawResultsJson = "{}"
                )
            )
        )
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is UiState.Success<TestReport>)
        val sectionsStatuses = viewModel.sections.value.map { it.status }
        assertEquals(listOf("PASS", "FAIL"), sectionsStatuses)

        viewModel.viewModelScope.cancel()
    }
}

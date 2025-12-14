package com.app.miklink.ui.test

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestSectionResult
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class TestViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val runTestUseCase: RunTestUseCase,
    private val reportRepository: ReportRepository
) : ViewModel() {

    // Logs removed from UI; keep Execution state only

    private val _uiState = MutableStateFlow<UiState<TestReport>>(UiState.Idle)
    val uiState: StateFlow<UiState<TestReport>> = _uiState.asStateFlow()

    private val _sections = MutableStateFlow<List<TestSection>>(emptyList())
    val sections: StateFlow<List<TestSection>> = _sections.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun startTest() {
        if (_isRunning.value) return

        val plan = buildPlan() ?: return

        viewModelScope.launch {
            _sections.value = emptyList()
            _uiState.value = UiState.Loading
            _isRunning.value = true

            runTestUseCase.execute(plan)
                .catch { throwable ->
                    handleFailure(throwable.message)
                }
                .collect { event ->
                    when (event) {
                        is TestEvent.SectionsUpdated -> _sections.value = mapSections(event.sections)
                        is TestEvent.Completed -> handleCompletion(plan, event.outcome)
                        is TestEvent.Failed -> handleFailure(event.error.message)
                        else -> {
                            // Ignore raw log/progress events for the UI (kept internal to UseCase)
                        }
                    }
                }
        }
    }

    fun saveReportToDb(report: TestReport) {
        viewModelScope.launch {
            reportRepository.saveReport(report)
        }
    }

    private fun handleCompletion(plan: TestPlan, outcome: TestOutcome) {
        _isRunning.value = false
        _sections.value = mapSections(outcome.sections)
        val report = buildReport(plan, outcome)
        _uiState.value = UiState.Success(report)
    }

    private fun handleFailure(message: String?) {
        _isRunning.value = false
        val errorMessage = message ?: "Errore sconosciuto"
        _uiState.value = UiState.Error(errorMessage)
    }

    private fun buildPlan(): TestPlan? {
        val clientId = readId("clientId")
        val profileId = readId("profileId")
        val socketNameRaw = savedStateHandle.get<String>("socketName") ?: ""
        val socketName = try {
            Uri.decode(socketNameRaw)
        } catch (_: Exception) {
            socketNameRaw
        }

        if (clientId <= 0 || profileId <= 0) {
            val message = "Parametri di test non validi. client=$clientId profile=$profileId"
            _uiState.value = UiState.Error("Parametri di navigazione non validi.")
            return null
        }

        return TestPlan(
            clientId = clientId,
            profileId = profileId,
            socketId = socketName,
            notes = null
        )
    }

    private fun buildReport(plan: TestPlan, outcome: TestOutcome): TestReport {
        return TestReport(
            reportId = 0L,
            clientId = plan.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = plan.socketId,
            notes = plan.notes,
            probeName = null,
            profileName = null,
            overallStatus = outcome.overallStatus,
            resultFormatVersion = 1,
            resultsJson = outcome.rawResultsJson ?: "{}"
        )
    }

    private fun mapSections(results: List<TestSectionResult>): List<TestSection> {
        return results.map { result ->
            val type = mapSectionType(result.type)
            TestSection(
                category = mapCategory(type),
                type = type,
                title = result.title.ifBlank { type.name },
                status = result.status,
                details = result.details.map { (label, value) -> TestDetail(label = label, value = value) }
            )
        }
    }

    private fun mapSectionType(rawType: String): TestSectionType {
        val normalized = rawType.trim().uppercase(Locale.ROOT)
        return TestSectionType.values().firstOrNull { it.name == normalized } ?: TestSectionType.NETWORK
    }

    private fun mapCategory(type: TestSectionType): TestSectionCategory {
        return when (type) {
            TestSectionType.NETWORK, TestSectionType.LLDP -> TestSectionCategory.INFO
            else -> TestSectionCategory.TEST
        }
    }

    private fun readId(key: String): Long {
        return savedStateHandle.get<Long>(key)
            ?: savedStateHandle.get<String>(key)?.toLongOrNull()
            ?: -1L
    }

    // appendLog removed: raw execution logs are not surfaced to the UI anymore
}

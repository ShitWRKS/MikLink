/*
 * Purpose: Orchestrate test execution UI state and delegate report persistence through domain use cases.
 * Inputs: SavedStateHandle navigation args (clientId, profileId, socketName) and RunTestUseCase events.
 * Outputs: UiState/log flows plus typed TestRunSnapshot for the UI and persisted reports via SaveTestReportUseCase.
 * Notes: Keeps UI free from repository details; persistence policy (Socket-ID increment) lives in the use case; log buffer is UI-only per ADR-0011.
 */
package com.app.miklink.ui.test

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.R
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.test.logging.ExecutionLogBuffer
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestRunSnapshot
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCase
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class TestViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val runTestUseCase: RunTestUseCase,
    private val saveTestReportUseCase: SaveTestReportUseCase
) : ViewModel() {

    // Logs removed from UI; keep Execution state only

    private val _uiState = MutableStateFlow<UiState<TestReport>>(UiState.Idle)
    val uiState: StateFlow<UiState<TestReport>> = _uiState.asStateFlow()

    private val _snapshot = MutableStateFlow<TestRunSnapshot?>(null)
    val snapshot: StateFlow<TestRunSnapshot?> = _snapshot.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val logBuffer = ExecutionLogBuffer()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    // Tracks the current test coroutine so it can be cancelled if a new test starts
    private var testJob: Job? = null

    companion object {
        // Global timeout to prevent indefinite test execution (e.g. stuck HTTP calls)
        private const val TEST_TIMEOUT_MS = 90_000L
        private const val TEST_TIMEOUT_SECONDS = (TEST_TIMEOUT_MS / 1000).toInt()
    }

    fun startTest() {
        // Cancel any previous test still running (prevents stale coroutines and UI lock)
        testJob?.cancel()

        val plan = buildPlan() ?: return

        testJob = viewModelScope.launch {
            _snapshot.value = null
            _uiState.value = UiState.Loading
            _isRunning.value = true
            logBuffer.clear()
            _logs.value = emptyList()

            try {
                withTimeout(TEST_TIMEOUT_MS) {
                    runTestUseCase.execute(plan)
                        .catch { throwable ->
                            if (throwable is CancellationException) throw throwable
                            handleFailure(throwable.message)
                        }
                        .collect { event ->
                            when (event) {
                                is TestEvent.Progress -> appendLog("[${event.progress.currentStep}] ${event.progress.message}")
                                is TestEvent.LogLine -> appendLog(event.message)
                                is TestEvent.SnapshotUpdated -> {
                                    _snapshot.value = event.snapshot
                                }
                                is TestEvent.Completed -> {
                                    _snapshot.value = event.outcome.finalSnapshot
                                    handleCompletion(plan, event.outcome)
                                }
                                is TestEvent.Failed -> handleFailure(event.error.message)
                            }
                        }
                }
            } catch (e: TimeoutCancellationException) {
                handleFailure(context.getString(R.string.test_timeout_error, TEST_TIMEOUT_SECONDS))
            } finally {
                // Ensures isRunning is reset even if cancelled or timed out
                _isRunning.value = false
            }
        }
    }

    fun saveReportToDb(report: TestReport) {
        viewModelScope.launch {
            saveTestReportUseCase(report, incrementClientCounter = true)
        }
    }

    private fun handleCompletion(plan: TestPlan, outcome: TestOutcome) {
        // Note: _isRunning reset moved to finally block in startTest()
        _snapshot.value = outcome.finalSnapshot
        val report = buildReport(plan, outcome)
        _uiState.value = UiState.Success(report)
    }

    private fun handleFailure(message: String?) {
        // Note: _isRunning reset moved to finally block in startTest()
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

    private fun appendLog(line: String) {
        logBuffer.append(line)
        _logs.value = logBuffer.snapshot()
    }

    private fun readId(key: String): Long {
        return savedStateHandle.get<Long>(key)
            ?: savedStateHandle.get<String>(key)?.toLongOrNull()
            ?: -1L
    }

    // appendLog removed: raw execution logs are not surfaced to the UI anymore
}

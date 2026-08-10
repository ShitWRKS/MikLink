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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

data class ReportSaveState(
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

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

    private val _reportSaveState = MutableStateFlow(ReportSaveState())
    val reportSaveState: StateFlow<ReportSaveState> = _reportSaveState.asStateFlow()

    // Tracks the current test coroutine so it can be cancelled if a new test starts
    private var testJob: Job? = null

    // Mutex serializes the start-replace sequence so two concurrent starts never interleave
    private val startMutex = Mutex()

    // Monotonic generation counter: only the current generation may touch UI state
    private var runGeneration: Long = 0L

    companion object {
        // Global timeout to prevent indefinite test execution (e.g. stuck HTTP calls)
        private const val TEST_TIMEOUT_MS = 90_000L
        private const val TEST_TIMEOUT_SECONDS = (TEST_TIMEOUT_MS / 1000).toInt()
    }

    fun startTest() {
        // Validate plan BEFORE cancelling any running test
        val plan = buildPlan() ?: return

        viewModelScope.launch {
            startMutex.withLock {
                val myGeneration = ++runGeneration

                // Cancel previous run and wait for it to fully terminate
                testJob?.cancelAndJoin()
                testJob = null

                testJob = viewModelScope.launch {
                    // Initialize state for the new run
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
                                    handleFailure(myGeneration, throwable.message)
                                }
                                .collect { event ->
                                    if (!isCurrentRun(myGeneration)) return@collect
                                    when (event) {
                                        is TestEvent.Progress -> appendLog(myGeneration, "[${event.progress.currentStep}] ${event.progress.message}")
                                        is TestEvent.LogLine -> appendLog(myGeneration, event.message)
                                        is TestEvent.SnapshotUpdated -> {
                                            if (isCurrentRun(myGeneration)) {
                                                _snapshot.value = event.snapshot
                                            }
                                        }
                                        is TestEvent.Completed -> {
                                            if (isCurrentRun(myGeneration)) {
                                                _snapshot.value = event.outcome.finalSnapshot
                                                handleCompletion(myGeneration, plan, event.outcome)
                                            }
                                        }
                                        is TestEvent.Failed -> handleFailure(myGeneration, event.error.message)
                                    }
                                }
                        }
                    } catch (e: TimeoutCancellationException) {
                        handleFailure(myGeneration, context.getString(R.string.test_timeout_error, TEST_TIMEOUT_SECONDS))
                    } finally {
                        if (isCurrentRun(myGeneration)) {
                            _isRunning.value = false
                        }
                    }
                }
            }
        }
    }

    fun saveReportToDb(report: TestReport) {
        if (_reportSaveState.value.isSaving) return
        viewModelScope.launch {
            _reportSaveState.value = ReportSaveState(isSaving = true)
            try {
                saveTestReportUseCase(report, incrementClientCounter = true)
                _reportSaveState.value = ReportSaveState(isSaved = true)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _reportSaveState.value = ReportSaveState(
                    errorMessage = context.getString(R.string.test_execution_save_error)
                )
            }
        }
    }

    private fun isCurrentRun(generation: Long): Boolean =
        generation == runGeneration

    private fun handleCompletion(generation: Long, plan: TestPlan, outcome: TestOutcome) {
        if (!isCurrentRun(generation)) return
        _snapshot.value = outcome.finalSnapshot
        val report = buildReport(plan, outcome)
        _uiState.value = UiState.Success(report)
    }

    private fun handleFailure(generation: Long, message: String?) {
        if (!isCurrentRun(generation)) return
        val errorMessage = message ?: "Errore sconosciuto"
        _uiState.value = UiState.Error(errorMessage)
    }

    private fun appendLog(generation: Long, line: String) {
        if (!isCurrentRun(generation)) return
        logBuffer.append(line)
        _logs.value = logBuffer.snapshot()
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
            _uiState.value = UiState.Error(context.getString(R.string.test_execution_invalid_navigation))
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
            resultsJson = outcome.rawResultsJson
        )
    }

    private fun readId(key: String): Long {
        return savedStateHandle.get<Long>(key)
            ?: savedStateHandle.get<String>(key)?.toLongOrNull()
            ?: -1L
    }
}

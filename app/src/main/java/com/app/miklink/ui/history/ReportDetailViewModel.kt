/*
 * Purpose: Provide report detail state and actions (edit, delete, repeat route, PDF status) for the legacy detail screen.
 * Inputs: SavedStateHandle reportId, repositories (report/client/profile), parse use case, repeat route builder, prefs.
 * Outputs: StateFlows for report data, parsed results, metadata fields, client name, pdf status, and actions to save/delete/repeat.
 */
package com.app.miklink.ui.history


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.report.ReportData
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository

import com.app.miklink.core.data.pdf.PdfGenerator
import com.app.miklink.core.data.pdf.PdfExportConfig
import com.app.miklink.core.domain.usecase.report.ParseReportResultsUseCase
import com.app.miklink.ui.navigation.RepeatTestRouteBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val clientRepository: ClientRepository,
    private val profileRepository: TestProfileRepository,

    private val pdfGenerator: PdfGenerator,
    private val parseReportResults: ParseReportResultsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val repeatTestRouteBuilder: RepeatTestRouteBuilder,
    savedStateHandle: SavedStateHandle
) : ViewModel(), ReportDetailScreenStateProvider {

    val pdfIncludeEmptyTests = userPreferencesRepository.pdfIncludeEmptyTests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val pdfSelectedColumns = userPreferencesRepository.pdfSelectedColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val pdfReportTitle = userPreferencesRepository.pdfReportTitle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Collaudo Cablaggio di Rete")

    val pdfHideEmptyColumns = userPreferencesRepository.pdfHideEmptyColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val reportId: Long = savedStateHandle.get<Long>("reportId") ?: -1L

    override val report: StateFlow<TestReport?> = reportRepository.observeAllReports()
        .map { reports -> reports.find { it.reportId == reportId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    override val socketName = MutableStateFlow("")
    override val notes = MutableStateFlow("")

    private val _parsedResults = MutableStateFlow<ReportData?>(null)
    override val parsedResults: StateFlow<ReportData?> = _parsedResults.asStateFlow()

    private val _clientName = MutableStateFlow("")
    override val clientName: StateFlow<String> = _clientName.asStateFlow()

    private val _profile = MutableStateFlow<TestProfile?>(null)
    val profile: StateFlow<TestProfile?> = _profile.asStateFlow()

    private val _pdfStatus = MutableStateFlow("")
    override val pdfStatus: StateFlow<String> = _pdfStatus.asStateFlow()

    init {
        viewModelScope.launch {
            report.collectLatest { currentReport ->
                if (currentReport != null) {
                    socketName.value = currentReport.socketName ?: ""
                    notes.value = currentReport.notes ?: ""
                    _parsedResults.value = parseReportResults(currentReport.resultsJson).getOrNull()
                    
                    // Load client name
                    currentReport.clientId?.let { clientId ->
                        val client = clientRepository.getClient(clientId)
                        _clientName.value = client?.companyName ?: "Unknown Client"
                    } ?: run {
                        _clientName.value = "Unknown Client"
                    }
                    
                    // Load profile
                    currentReport.profileName?.let { profileName ->
                        _profile.value = profileRepository.observeAllProfiles().firstOrNull()?.find { it.profileName == profileName }
                    } ?: run {
                        _profile.value = null
                    }
                }
            }
        }
    }

    override fun updateReportDetails() {
        viewModelScope.launch {
            report.value?.let {
                val updatedReport = it.copy(socketName = socketName.value, notes = notes.value)
                reportRepository.saveReport(updatedReport)
            }
        }
    }



    suspend fun getProposedFilename(): String {
        val currentReport = report.value ?: return "MikLink_Report"
        val client = currentReport.clientId?.let { id -> clientRepository.getClient(id) }
        val clientName = client?.companyName?.replace(" ", "_") ?: "Client"
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date(currentReport.timestamp))
        return "${clientName}-${date}-${currentReport.reportId}"
    }
    
    // Generate PDF File for single test using iText with Config
    suspend fun generatePdfWithIText(config: PdfExportConfig): java.io.File? {
        val currentReport = report.value ?: return null
        val client = currentReport.clientId?.let { it -> clientRepository.getClient(it) }
        
        return try {
            pdfGenerator.generatePdfReport(
                rawReports = listOf(currentReport),
                client = client,
                config = config
            )
        } catch (e: Exception) {
            android.util.Log.e("ReportDetailVM", "Error generating PDF", e)
            null
        }
    }



    override fun exportReportToPdf() {
        viewModelScope.launch {
            val currentReport = report.value
            if (currentReport == null) {
                _pdfStatus.value = "Report non disponibile"
                return@launch
            }

            val profileForReport = currentReport.profileName?.let { name ->
                profileRepository.observeAllProfiles().firstOrNull()?.find { it.profileName == name }
            }
            val client = currentReport.clientId?.let { clientRepository.getClient(it) }

            val file = withContext(Dispatchers.IO) {
                runCatching {
                    pdfGenerator.generateSingleTestPdf(
                        report = currentReport,
                        client = client,
                        profile = profileForReport,
                        reportTitle = pdfReportTitle.value
                    )
                }.getOrNull()
            }
            _pdfStatus.value = file?.let { "PDF generato: ${it.absolutePath}" }
                ?: "Impossibile generare il PDF"
        }
    }

    override suspend fun generatePdf(config: PdfExportConfig): java.io.File? {
        return generatePdfWithIText(config)
    }

    override suspend fun buildRepeatRoute(): String? = withContext(Dispatchers.IO) {
        repeatTestRouteBuilder.build(report.value)
    }

    override suspend fun deleteReport() {
        withContext(Dispatchers.IO) {
            report.value?.let { reportRepository.deleteReport(it) }
        }
    }

}

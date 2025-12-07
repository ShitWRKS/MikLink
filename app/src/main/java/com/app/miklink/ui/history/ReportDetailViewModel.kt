package com.app.miklink.ui.history


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.db.model.TestProfile

import com.app.miklink.data.pdf.PdfGeneratorIText
import com.app.miklink.ui.history.model.ParsedResults
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val reportDao: ReportDao,
    private val clientDao: ClientDao,
    private val profileDao: TestProfileDao,

    private val pdfGeneratorIText: PdfGeneratorIText, // For new single-test PDF
    private val moshi: Moshi,
    private val userPreferencesRepository: com.app.miklink.data.repository.UserPreferencesRepository,
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

    override val report: StateFlow<Report?> = reportDao.getReportById(reportId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    override val socketName = MutableStateFlow("")
    override val notes = MutableStateFlow("")

    private val _parsedResults = MutableStateFlow<ParsedResults?>(null)
    override val parsedResults: StateFlow<ParsedResults?> = _parsedResults.asStateFlow()

    private val _clientName = MutableStateFlow("")
    val clientName: StateFlow<String> = _clientName.asStateFlow()

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
                    _parsedResults.value = parseResults(currentReport.resultsJson)
                    
                    // Load client name
                    currentReport.clientId?.let { clientId ->
                        val client = clientDao.getClientById(clientId).firstOrNull()
                        _clientName.value = client?.companyName ?: "Unknown Client"
                    } ?: run {
                        _clientName.value = "Unknown Client"
                    }
                    
                    // Load profile
                    currentReport.profileName?.let { profileName ->
                        _profile.value = profileDao.getProfileByName(profileName).firstOrNull()
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
                reportDao.update(updatedReport)
            }
        }
    }



    suspend fun getProposedFilename(): String {
        val currentReport = report.value ?: return "MikLink_Report"
        val client = currentReport.clientId?.let { id -> clientDao.getClientById(id).firstOrNull() }
        val clientName = client?.companyName?.replace(" ", "_") ?: "Client"
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date(currentReport.timestamp))
        return "${clientName}-${date}-${currentReport.reportId}"
    }
    
    // Generate PDF File for single test using iText with Config
    suspend fun generatePdfWithIText(config: com.app.miklink.data.pdf.PdfExportConfig): java.io.File? {
        val currentReport = report.value ?: return null
        val client = currentReport.clientId?.let { it -> clientDao.getClientById(it).firstOrNull() }
        
        return try {
            pdfGeneratorIText.generatePdfReport(
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
        // No-op: la stampa è demandata alla UI
        _pdfStatus.value = ""
    }

    private fun parseResults(json: String): ParsedResults? {
        return try {
            moshi.adapter(ParsedResults::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}

package com.app.miklink.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.data.pdf.PdfGenerator
import com.app.miklink.core.data.pdf.PdfExportConfig
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.ui.history.model.ReportsByClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.withContext

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val clientRepository: ClientRepository,
    private val pdfGenerator: PdfGenerator,
    private val probeRepository: ProbeRepository,
    private val profileRepository: TestProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val pdfIncludeEmptyTests = userPreferencesRepository.pdfIncludeEmptyTests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val pdfSelectedColumns = userPreferencesRepository.pdfSelectedColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val pdfReportTitle = userPreferencesRepository.pdfReportTitle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Collaudo Cablaggio di Rete")

    val pdfHideEmptyColumns = userPreferencesRepository.pdfHideEmptyColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reports: StateFlow<List<TestReport>> = reportRepository.observeAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<Client>> = clientRepository.observeAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reportsByClient = MutableStateFlow<List<ReportsByClient>>(emptyList())
    val reportsByClient = _reportsByClient.asStateFlow()

    private val _pdfStatus = MutableStateFlow("")
    val pdfStatus = _pdfStatus.asStateFlow()

    // Search and Filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow<FilterStatus>(FilterStatus.ALL)
    val filterStatus = _filterStatus.asStateFlow()

    init {
        // Group reports by client with search and filter
        viewModelScope.launch {
            combine(
                reportRepository.observeAllReports(),
                clientRepository.observeAllClients(),
                _searchQuery,
                _filterStatus
            ) { reports, clients, query, status ->
                val clientMap = clients.associateBy { it.clientId }
                
                // Apply filters
                val filteredReports = reports.filter { report ->
                    // Status filter
                    val matchesStatus = when (status) {
                        FilterStatus.ALL -> true
                        FilterStatus.PASS -> report.overallStatus == "PASS"
                        FilterStatus.FAIL -> report.overallStatus == "FAIL"
                    }
                    
                    // Search filter
                    val matchesSearch = if (query.isBlank()) {
                        true
                    } else {
                        val lowerQuery = query.lowercase()
                        val socketMatch = report.socketName?.lowercase()?.contains(lowerQuery) ?: false
                        val clientMatch = report.clientId?.let { 
                            clientMap[it]?.companyName?.lowercase()?.contains(lowerQuery) 
                        } ?: false
                        val notesMatch = report.notes?.lowercase()?.contains(lowerQuery) ?: false
                        socketMatch || clientMatch || notesMatch
                    }
                    
                    matchesStatus && matchesSearch
                }
                
                filteredReports.groupBy { it.clientId }
                    .map { (clientId, clientReports) ->
                        ReportsByClient(
                            client = clientId?.let { clientMap[it] },
                            reports = clientReports.sortedByDescending { it.timestamp }
                        )
                    }
                    .sortedByDescending { it.lastTestDate }
            }.collectLatest { grouped ->
                _reportsByClient.value = grouped
            }
        }
    }

    fun deleteReport(reportId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val report = reportRepository.getReport(reportId)
                report?.let { reportRepository.deleteReport(it) }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error deleting report", e)
            }
        }
    }

    fun duplicateReport(reportId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val original = reportRepository.getReport(reportId)
                original?.let {
                    val duplicate = it.copy(
                        reportId = 0,
                        timestamp = System.currentTimeMillis()
                    )
                    reportRepository.saveReport(duplicate)
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error duplicating report", e)
            }
        }
    }

    /**
     * Generate PDF using iText 7 for client reports from history.
     */
    suspend fun generatePdfWithITextForClient(
        clientReports: ReportsByClient,
        config: PdfExportConfig
    ): java.io.File? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            pdfGenerator.generatePdfReport(clientReports.reports, clientReports.client, config)
        }
    }

    /**
     * Generate PDF using iText 7 for a single report with custom config.
     */
    suspend fun generatePdfForSingleReport(
        report: TestReport,
        config: PdfExportConfig
    ): java.io.File? {
        // Find client for this report
        val client = report.clientId?.let { clientId -> clientRepository.getClient(clientId) }
        
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            pdfGenerator.generatePdfReport(listOf(report), client, config)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterStatus(status: FilterStatus) {
        _filterStatus.value = status
    }

    /**
     * Get navigation route for repeating a test with the same parameters.
     * Returns null if probe or profile cannot be found.
     * Note: ProbeConfig doesn't store a name, so we get the first available probe.
     */
    suspend fun getRepeatTestRoute(report: TestReport): String? = withContext(Dispatchers.IO) {
        try {
            val probe = probeRepository.observeProbeConfig().first()
            val profile = profileRepository.observeAllProfiles().first().firstOrNull {
                it.profileName == report.profileName
            }
            
            if (probe != null && profile != null && report.clientId != null) {
                val encodedSocket = android.net.Uri.encode(report.socketName ?: "")
                "test_execution/${report.clientId}/${profile.profileId}/$encodedSocket"
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("HistoryViewModel", "Error getting repeat test route", e)
            null
        }
    }
}

enum class FilterStatus {
    ALL, PASS, FAIL
}

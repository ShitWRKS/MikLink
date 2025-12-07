package com.app.miklink.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.pdf.PdfGenerator
import com.app.miklink.data.pdf.PdfGeneratorIText
import com.app.miklink.ui.history.model.ReportsByClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.print.PrintDocumentAdapter
import kotlinx.coroutines.withContext

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val reportDao: ReportDao,
    private val clientDao: ClientDao,
    private val pdfGenerator: PdfGenerator,
    private val pdfGeneratorIText: PdfGeneratorIText,
    private val probeDao: com.app.miklink.data.db.dao.ProbeConfigDao,
    private val profileDao: com.app.miklink.data.db.dao.TestProfileDao
) : ViewModel() {

    val reports: StateFlow<List<Report>> = reportDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<Client>> = clientDao.getAllClients()
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
                reportDao.getAllReports(),
                clientDao.getAllClients(),
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
                val report = reportDao.getReportByIdOnce(reportId)
                report?.let { reportDao.delete(it) }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error deleting report", e)
            }
        }
    }

    fun duplicateReport(reportId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val original = reportDao.getReportByIdOnce(reportId)
                original?.let {
                    val duplicate = it.copy(
                        reportId = 0,
                        timestamp = System.currentTimeMillis()
                    )
                    reportDao.insert(duplicate)
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error duplicating report", e)
            }
        }
    }

    // Nuove API per la stampa dalla UI
    fun generateHtmlForClientReports(clientReports: ReportsByClient): String {
        // Generate a filename/title for the PDF
        val clientName = clientReports.client?.companyName?.replace(" ", "_") ?: "Client"
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val title = "${clientName}_Reports_${date}"
        
        return pdfGenerator.generateHtmlFromReports(clientReports.reports, clientReports.client, title)
    }

    suspend fun createPrintAdapter(context: android.content.Context, html: String, jobName: String): PrintDocumentAdapter =
        pdfGenerator.createPrintAdapter(context, html, jobName)

    /**
     * Generate PDF using iText 7 for client reports from history.
     */
    suspend fun generatePdfWithITextForClient(clientReports: ReportsByClient): java.io.File? {
        val clientName = clientReports.client?.companyName?.replace(" ", "_") ?: "Client"
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val title = "${clientName}_Reports_${date}"
        
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            pdfGeneratorIText.generatePdfReport(clientReports.reports, clientReports.client, title)
        }
    }

    /**
     * Generate PDF using iText 7 for a single report.
     */
    suspend fun generatePdfForSingleReport(report: Report): java.io.File? {
        // Find client for this report
        val client = report.clientId?.let { clientId ->
            clientDao.getClientById(clientId).first()
        }
        
        // Find profile for this report
        val profile = report.profileName?.let { profileName ->
            profileDao.getProfileByName(profileName).first()
        }
        
        val clientName = client?.companyName?.replace(" ", "_") ?: "Client"
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date(report.timestamp))
        val title = "${clientName}-${date}-${report.reportId}"
        
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            pdfGeneratorIText.generateSingleTestPdf(report, client, profile, title)
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
    suspend fun getRepeatTestRoute(report: Report): String? = withContext(Dispatchers.IO) {
        try {
            // Get first available probe (since ProbeConfig doesn't have a name field)
            val probe = probeDao.getAllProbes().first().firstOrNull()
            
            // Get profile ID by profileName
            val profile = profileDao.getAllProfiles().first().firstOrNull {
                it.profileName == report.profileName
            }
            
            if (probe != null && profile != null && report.clientId != null) {
                val encodedSocket = android.net.Uri.encode(report.socketName ?: "")
                "test_execution/${report.clientId}/${probe.probeId}/${profile.profileId}/$encodedSocket"
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

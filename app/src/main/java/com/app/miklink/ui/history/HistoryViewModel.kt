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

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val reportDao: ReportDao,
    private val clientDao: ClientDao,
    private val pdfGenerator: PdfGenerator,
    private val pdfGeneratorIText: PdfGeneratorIText
) : ViewModel() {

    val reports: StateFlow<List<Report>> = reportDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<Client>> = clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reportsByClient = MutableStateFlow<List<ReportsByClient>>(emptyList())
    val reportsByClient = _reportsByClient.asStateFlow()

    private val _pdfStatus = MutableStateFlow("")
    val pdfStatus = _pdfStatus.asStateFlow()

    init {
        // Group reports by client
        viewModelScope.launch {
            combine(
                reportDao.getAllReports(),
                clientDao.getAllClients()
            ) { reports, clients ->
                val clientMap = clients.associateBy { it.clientId }
                reports.groupBy { it.clientId }
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
}

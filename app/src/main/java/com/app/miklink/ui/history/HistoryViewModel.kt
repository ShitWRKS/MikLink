package com.app.miklink.ui.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.pdf.PdfGenerator
import com.app.miklink.ui.history.model.ReportsByClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val reportDao: ReportDao,
    private val clientDao: ClientDao,
    private val pdfGenerator: PdfGenerator
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

    fun exportClientReports(clientReports: ReportsByClient, uri: Uri) {
        viewModelScope.launch {
            _pdfStatus.value = "Generating batch PDF..."
            try {
                pdfGenerator.createBatchPdf(clientReports.reports, clientReports.client, uri)
                _pdfStatus.value = "PDF saved successfully (${clientReports.reports.size} reports)"
            } catch (e: Exception) {
                _pdfStatus.value = "Error: ${e.message}"
                android.util.Log.e("HistoryViewModel", "Error creating batch PDF", e)
            }
        }
    }

    fun exportProjectReportToPdf(clientId: Long, uri: Uri) {
        viewModelScope.launch {
            _pdfStatus.value = "Exporting..."
            val clientReports = reportDao.getReportsForClient(clientId).firstOrNull() ?: emptyList()
            val client = clientDao.getClientById(clientId).firstOrNull()

            if (clientReports.isNotEmpty()) {
                try {
                    pdfGenerator.createBatchPdf(clientReports, client, uri)
                    _pdfStatus.value = "Project Report saved successfully!"
                } catch (e: Exception) {
                    _pdfStatus.value = "Error: ${e.message}"
                    android.util.Log.e("HistoryViewModel", "Error creating project PDF", e)
                }
            } else {
                _pdfStatus.value = "No reports found for this client."
            }
        }
    }
}

package com.app.miklink.ui.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.pdf.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _pdfStatus = MutableStateFlow("")
    val pdfStatus = _pdfStatus.asStateFlow()

    fun exportProjectReportToPdf(clientId: Long, uri: Uri) {
        viewModelScope.launch {
            _pdfStatus.value = "Exporting..."
            val clientReports = reportDao.getReportsForClient(clientId).firstOrNull() ?: emptyList()
            val client = clientDao.getClientById(clientId).firstOrNull()

            if (clientReports.isNotEmpty()) {
                val htmlContent = pdfGenerator.populateProjectReportTemplate(clientReports, client)
                pdfGenerator.createPdf(htmlContent, uri)
                _pdfStatus.value = "Project Report saved successfully!"
            } else {
                _pdfStatus.value = "No reports found for this client."
            }
        }
    }
}

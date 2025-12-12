package com.app.miklink.ui.client


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.local.room.v1.dao.ClientDao
import com.app.miklink.core.data.local.room.v1.dao.ReportDao
import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.data.pdf.ExportColumn
import com.app.miklink.data.pdf.PdfExportConfig
import com.app.miklink.data.pdf.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class ClientListViewModel @Inject constructor(
    private val clientDao: ClientDao,
    private val reportDao: ReportDao,

    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    val clients: StateFlow<List<Client>> = clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pdfStatus = MutableStateFlow("")
    val pdfStatus = _pdfStatus.asStateFlow()

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            clientDao.delete(client)
        }
    }



    /**
     * Generate PDF using iText 7 (alternative to HTML/WebView approach).
     * Returns a File object pointing to the generated PDF in cache directory.
     */
    suspend fun generatePdfWithIText(clientId: Long): java.io.File? {
        val reports = reportDao.getReportsForClient(clientId).firstOrNull() ?: emptyList()
        if (reports.isEmpty()) return null
        
        val client = clientDao.getClientById(clientId).firstOrNull()
        val clientName = client?.companyName?.replace(" ", "_") ?: "Client"
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val title = "${clientName}_Reports_${date}"
        
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val config = PdfExportConfig(
                title = title,
                includeEmptyTests = true,
                columns = ExportColumn.values().toList(),
                showSignatures = false,
                signatureLeftLabel = "",
                signatureRightLabel = ""
            )
            pdfGenerator.generatePdfReport(reports, client, config)
        }
    }
}

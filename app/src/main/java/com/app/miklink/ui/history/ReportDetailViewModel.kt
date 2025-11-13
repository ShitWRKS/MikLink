package com.app.miklink.ui.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.pdf.PdfGenerator
import com.app.miklink.ui.history.model.ParsedResults
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val reportDao: ReportDao,
    private val clientDao: ClientDao,
    private val pdfGenerator: PdfGenerator, // Injected dependency
    private val moshi: Moshi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reportId: Long = savedStateHandle.get<Long>("reportId") ?: -1L

    val report: StateFlow<Report?> = reportDao.getReportById(reportId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val socketName = MutableStateFlow("")
    val notes = MutableStateFlow("")

    private val _parsedResults = MutableStateFlow<ParsedResults?>(null)
    val parsedResults = _parsedResults.asStateFlow()
    
    private val _pdfStatus = MutableStateFlow("")
    val pdfStatus = _pdfStatus.asStateFlow()

    init {
        viewModelScope.launch {
            report.collectLatest { currentReport ->
                if (currentReport != null) {
                    socketName.value = currentReport.socketName ?: ""
                    notes.value = currentReport.notes ?: ""
                    _parsedResults.value = parseResults(currentReport.resultsJson)
                }
            }
        }
    }

    fun updateReportDetails() {
        viewModelScope.launch {
            report.value?.let {
                val updatedReport = it.copy(socketName = socketName.value, notes = notes.value)
                reportDao.update(updatedReport)
            }
        }
    }
    
    fun exportReportToPdf(uri: Uri) {
        viewModelScope.launch {
            val currentReport = report.value ?: return@launch
            val client = currentReport.clientId?.let { clientDao.getClientById(it).firstOrNull() }

            _pdfStatus.value = "Generating PDF..."
            try {
                val html = pdfGenerator.populateSingleReportTemplate(currentReport, client)
                pdfGenerator.createPdf(html, uri)
                _pdfStatus.value = "PDF saved successfully."
            } catch (e: Exception) {
                _pdfStatus.value = "Error: ${e.message}"
            }
        }
    }

    private fun parseResults(json: String): ParsedResults? {
        return try {
            moshi.adapter(ParsedResults::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}

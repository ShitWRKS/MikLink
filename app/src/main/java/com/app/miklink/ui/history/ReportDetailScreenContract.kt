package com.app.miklink.ui.history

import com.app.miklink.core.data.local.room.v1.model.Report
import com.app.miklink.ui.history.model.ParsedResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Contratto per permettere test/fake del ReportDetailScreen senza dipendenze Hilt reali. */
interface ReportDetailScreenStateProvider {
    val report: StateFlow<Report?>
    val parsedResults: StateFlow<ParsedResults?>
    val pdfStatus: StateFlow<String>
    val socketName: MutableStateFlow<String>
    val notes: MutableStateFlow<String>
    fun updateReportDetails()
    fun exportReportToPdf()
}

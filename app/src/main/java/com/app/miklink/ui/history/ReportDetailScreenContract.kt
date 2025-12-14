package com.app.miklink.ui.history

import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.model.report.ReportData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Contratto per permettere test/fake del ReportDetailScreen senza dipendenze Hilt reali. */
interface ReportDetailScreenStateProvider {
    val report: StateFlow<TestReport?>
    val parsedResults: StateFlow<ReportData?>
    val pdfStatus: StateFlow<String>
    val socketName: MutableStateFlow<String>
    val notes: MutableStateFlow<String>
    fun updateReportDetails()
    fun exportReportToPdf()
}

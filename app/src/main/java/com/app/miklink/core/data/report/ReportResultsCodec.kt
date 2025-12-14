package com.app.miklink.core.data.report

import com.app.miklink.core.domain.model.report.ReportData

interface ReportResultsCodec {
    fun decode(json: String): Result<ReportData>
    fun encode(data: ReportData): Result<String>
}

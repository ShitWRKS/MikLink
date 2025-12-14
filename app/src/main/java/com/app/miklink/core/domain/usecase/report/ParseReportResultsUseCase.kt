package com.app.miklink.core.domain.usecase.report

import com.app.miklink.core.domain.model.report.ReportData
import com.app.miklink.core.data.report.ReportResultsCodec
import javax.inject.Inject

interface ParseReportResultsUseCase {
    operator fun invoke(json: String): Result<ReportData>
}

class ParseReportResultsUseCaseImpl @Inject constructor(
    private val codec: ReportResultsCodec
) : ParseReportResultsUseCase {
    override fun invoke(json: String): Result<ReportData> = codec.decode(json)
}

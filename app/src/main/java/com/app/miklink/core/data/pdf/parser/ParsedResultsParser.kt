package com.app.miklink.core.data.pdf.parser

import com.app.miklink.core.data.report.ReportResultsCodec
import com.app.miklink.core.domain.model.report.ReportData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses serialized report results JSON into domain `ReportData` instances.
 * Delegates to `ReportResultsCodec` to stay aligned with the primary encode/decode path.
 */
@Singleton
class ParsedResultsParser @Inject constructor(
    private val reportResultsCodec: ReportResultsCodec
) {

    fun parse(json: String): ReportData? {
        if (json.isBlank()) return null
        return reportResultsCodec.decode(json).getOrNull()
    }
}


package com.app.miklink.core.data.pdf

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.model.TestProfile
import java.io.File

/**
 * Interface contract for PDF generation implementations.
 * Keep the contract small and testable so different generators (iText, HTML, etc.)
 * can be swapped with DI and covered by unit tests.
 */
interface PdfGenerator {
    fun generatePdfReport(rawReports: List<TestReport>, client: Client?, config: PdfExportConfig): File?

    fun generateSingleTestPdf(report: TestReport, client: Client?, profile: TestProfile?, reportTitle: String): File?
}


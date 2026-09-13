package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.data.pdf.ExportColumn
import com.app.miklink.core.data.pdf.PdfExportConfig
import com.app.miklink.core.data.pdf.PdfPageOrientation
import com.app.miklink.e2e.support.ScenarioRule
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("pdf-export")

    @Test
    fun productionGeneratorCreatesStructurallyValidPortraitAndLandscapeReports() = withCoreFixtures("pdf-export", scenarioRule::recordCleanup) { deps, fixtures ->
        assertCatalogMembership("pdf-export", FeatureGroup.PDF_EXPORT)
        val reportForPdf = fixtures.report.copy(socketName = "E2E-001", overallStatus = "PASS")
        val clientForPdf = fixtures.client.copy(companyName = "E2E Client")

        PdfPageOrientation.entries.forEach { orientation ->
            val title = "E2E ${orientation.name.lowercase().replaceFirstChar(Char::uppercase)}"
            val config = PdfExportConfig(
                title = title,
                includeEmptyTests = true,
                columns = ExportColumn.entries,
                showSignatures = true,
                signatureLeftLabel = "Technician",
                signatureRightLabel = "Customer",
                orientation = orientation,
                hideEmptyColumns = false
            )
            val file = requireNotNull(
                deps.pdfGenerator().generatePdfReport(listOf(reportForPdf), clientForPdf, config)
            )
            try {
                assertTrue(file.exists())
                assertTrue(file.length() > 100)
                val bytes = file.inputStream().use { it.readNBytes(5) }
                assertEquals("%PDF-", bytes.toString(Charsets.US_ASCII))

                PdfDocument(PdfReader(file)).use { pdf ->
                    assertTrue("Generated PDF must contain at least one page", pdf.numberOfPages >= 1)
                    val firstPageSize = pdf.getPage(1).pageSize
                    when (orientation) {
                        PdfPageOrientation.PORTRAIT -> assertTrue(firstPageSize.height > firstPageSize.width)
                        PdfPageOrientation.LANDSCAPE -> assertTrue(firstPageSize.width > firstPageSize.height)
                    }

                    val extractedText = (1..pdf.numberOfPages).joinToString("\n") { pageNumber ->
                        PdfTextExtractor.getTextFromPage(pdf.getPage(pageNumber))
                    }
                    assertTrue("Report title/header is missing", extractedText.contains(title))
                    assertTrue("Client header is missing", extractedText.contains(clientForPdf.companyName))
                    assertTrue(
                        "Results table socket is missing",
                        extractedText.contains(requireNotNull(reportForPdf.socketName))
                    )
                    assertTrue(
                        "Results table status is missing",
                        extractedText.contains(requireNotNull(reportForPdf.overallStatus))
                    )
                    assertTrue("Signature footer is missing", extractedText.contains("Technician"))
                    assertTrue("Generation footer is missing", extractedText.contains("Generato il"))
                    assertTrue("Page number footer is missing", extractedText.contains("Pag. 1"))
                }

                val trailer = file.inputStream().use { input ->
                    input.readBytes().takeLast(64).toByteArray().toString(Charsets.ISO_8859_1)
                }
                assertTrue("Generated PDF must have a readable EOF marker", trailer.contains("%%EOF"))
            } finally {
                assertTrue("Generated PDF cleanup failed", file.delete() || !file.exists())
            }
        }
    }
}

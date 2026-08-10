package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.data.pdf.ExportColumn
import com.app.miklink.core.data.pdf.PdfExportConfig
import com.app.miklink.core.data.pdf.PdfPageOrientation
import com.app.miklink.e2e.support.ScenarioRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("pdf-export")

    @Test
    fun preferencesGenerationRetrievalSignatureAndBasicOpenContract() = withCoreFixtures("pdf-export", scenarioRule::recordCleanup) { deps, fixtures ->
        assertCatalogMembership("pdf-export", FeatureGroup.PDF_EXPORT)
        val config = PdfExportConfig(
            title = "e2e-pdf-${System.nanoTime()}",
            includeEmptyTests = true,
            columns = ExportColumn.entries,
            showSignatures = true,
            signatureLeftLabel = "Technician",
            signatureRightLabel = "Customer",
            orientation = PdfPageOrientation.PORTRAIT,
            hideEmptyColumns = false
        )
        val file = requireNotNull(deps.pdfGenerator().generatePdfReport(listOf(fixtures.report), fixtures.client, config))
        try {
            assertTrue(file.exists())
            assertTrue(file.length() > 100)
            val bytes = file.inputStream().use { it.readNBytes(5) }
            assertEquals("%PDF-", bytes.toString(Charsets.US_ASCII))
            val trailer = file.inputStream().use { input ->
                val all = input.readBytes()
                all.takeLast(64).toByteArray().toString(Charsets.ISO_8859_1)
            }
            assertTrue("Generated PDF must have a readable EOF marker", trailer.contains("%%EOF"))
        } finally {
            file.delete()
        }
    }
}

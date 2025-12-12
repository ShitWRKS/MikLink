package com.app.miklink.data.pdf

import android.content.Context
import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.Report
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import com.app.miklink.ui.history.model.ParsedResults
import com.app.miklink.utils.normalizeTime
import com.app.miklink.utils.normalizeLinkSpeed
import com.app.miklink.utils.normalizeLinkStatus
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF Generator using iText 7 for professional PDF generation with advanced table handling.
 * Licensed under AGPL v3 - suitable for open source projects.
 */
@Singleton
class PdfGeneratorIText @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) : PdfGenerator {

    // instantiate a parser helper internally to avoid changing constructor signature that
    // annotation processors (KSP/Hilt) may attempt to resolve during processing.
    private val parsedResultsParser: ParsedResultsParser = ParsedResultsParser(moshi)


    private data class ColumnFlags(
        val ping: Boolean,
        val tdr: Boolean,
        val speed: Boolean,
        val linkSpeed: Boolean,
        val neighbor: Boolean,
        val showCpuWarning: Boolean
    )
    
    // Page number event handler is provided by PdfDocumentHelper.PageNumberEventHandler to avoid duplication

    /**
     * Generate PDF report from list of reports and client data.
     * Returns a File object pointing to the generated PDF in cache directory.
     */
    private val helper = PdfDocumentHelper()

    /**
     * Generate PDF report from list of reports and client data.
     * Returns a File object pointing to the generated PDF in cache directory.
     */
    override fun generatePdfReport(
        rawReports: List<Report>,
        client: Client?,
        config: PdfExportConfig
    ): File? {
        val filteredReports = if (config.includeEmptyTests) {
            rawReports
        } else {
            rawReports.filter { it.overallStatus != "SKIPPED" && it.resultsJson.length > 10 }
        }

        if (filteredReports.isEmpty()) return null

        val fileName = "${config.title}.pdf"
        val file = File(context.cacheDir, fileName)

        try {
            val writer = com.itextpdf.kernel.pdf.PdfWriter(FileOutputStream(file))
            val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
            
            val pageSize = if (config.orientation == PdfPageOrientation.PORTRAIT) 
                com.itextpdf.kernel.geom.PageSize.A4 
            else 
                com.itextpdf.kernel.geom.PageSize.A4.rotate()
                
            val document = Document(pdf, pageSize)
            document.setMargins(20f, 20f, 60f, 20f) // Increased bottom margin for footer

            // Add Header/Footer Event Handler (Page Numbers)
            pdf.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE, PdfDocumentHelper.PageNumberEventHandler())

            // 1. Load Logo
            var logoData: com.itextpdf.io.image.ImageData? = null
            try {
                // Try to load specific branding logo
                val logoId = context.resources.getIdentifier("logo_miklink_black", "drawable", context.packageName)
                val resId = if (logoId != 0) logoId else com.app.miklink.R.mipmap.ic_launcher
                
                val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                logoData = com.itextpdf.io.image.ImageDataFactory.create(stream.toByteArray())
            } catch (e: Exception) {
                // Ignore logo load error
            }

            // 2. Header (Client Info & Logo)
            helper.addHeader(document, client?.companyName, config.title, logoData)

            // 3. Results Table
            addResultsTable(document, filteredReports, config)

            // 4. Footer (Signatures, Notes, Warnings) - Fixed at bottom of last page
            val showCpuWarning = filteredReports.any { r ->
                 val parsed = parseResults(r.resultsJson)
                 parsed?.speedTest?.let { st ->
                     val probe = listOf(
                         st.status, st.ping, st.jitter, st.loss, st.tcpDownload, st.tcpUpload, st.udpDownload, st.udpUpload, st.warning
                     ).joinToString(" ") { it ?: "" }
                     probe.contains("local-cpu-load:100%", ignoreCase = true) ||
                     probe.contains("remote-cpu-load:100%", ignoreCase = true)
                 } == true
            }
            
            val footerTable = helper.createFooterTable(client?.notes, showCpuWarning, config)
            val pageNum = pdf.numberOfPages
            val pageParams = pdf.getPage(pageNum).pageSize
            val bottomMargin = 20f
            
            footerTable.setFixedPosition(pageNum, pageParams.left + 20f, pageParams.bottom + bottomMargin, pageParams.width - 40f)
            document.add(footerTable)

            document.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Generate a PDF for a single test report.
     * Delegates to generatePdfReport with a default configuration.
     */
    override fun generateSingleTestPdf(
        report: Report,
        client: Client?,
        profile: TestProfile?,
        reportTitle: String
    ): File? {
        val config = PdfExportConfig(
            title = reportTitle,
            includeEmptyTests = true,
            columns = ExportColumn.values().toList(),
            showSignatures = true,
            signatureLeftLabel = "Tecnico",
            signatureRightLabel = "Cliente",
            orientation = PdfPageOrientation.PORTRAIT // Default for single is Portrait usually better
        )
        return generatePdfReport(listOf(report), client, config)
    }



    private fun addResultsTable(document: Document, reports: List<Report>, config: PdfExportConfig) {
        // Section title
        // Note: replaced non-ASCII emoji with ASCII text to ensure consistent iText rendering and avoid font issues
        document.add(Paragraph("Dettaglio Test")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(10f)
            .setFontColor(DeviceRgb(102, 102, 102))
            .setMarginBottom(12f))

        if (reports.isEmpty()) {
            document.add(Paragraph("Nessun test presente nel report.")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE))
                .setFontSize(10f)
                .setFontColor(DeviceRgb(150, 150, 150)))
            return
        }

        // Filter columns if hideEmptyColumns is enabled
        val activeColumns = if (config.hideEmptyColumns) {
            config.columns.filter { col ->
                // Keep column if AT LEAST ONE report has data for it
                reports.any { report -> hasDataForColumn(report, col) }
            }
        } else {
            config.columns
        }

        if (activeColumns.isEmpty()) {
             document.add(Paragraph("Nessuna colonna selezionata o dati non disponibili.")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE))
                .setFontSize(10f))
             return
        }

        // Calculate column widths dynamically based on activeColumns
        // Base Unit for relative width.
        // LinkSpeed=12, Neighbor=18, Ping=15, TDR=10, Speed=18
        // Socket=12, Date=18, Status=12
        val widthMap = mapOf(
            ExportColumn.SOCKET to 12f,
            ExportColumn.DATE to 18f,
            ExportColumn.STATUS to 12f,
            ExportColumn.LINK_SPEED to 12f,
            ExportColumn.NEIGHBOR to 18f,
            ExportColumn.PING to 15f,
            ExportColumn.TDR to 10f,
            ExportColumn.SPEED_TEST to 18f
        )
        
        val columnWidths = activeColumns.map { widthMap[it] ?: 10f }.toFloatArray()
        
        val table = Table(UnitValue.createPercentArray(columnWidths))
            .setWidth(UnitValue.createPercentValue(100f))

        // Add header
        activeColumns.forEach { col ->
            val cell = Cell()
                .add(Paragraph(col.label)
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(DeviceRgb(241, 243, 244))
                .setBorder(SolidBorder(DeviceRgb(221, 221, 221), 1f))
                .setPadding(8f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
            table.addHeaderCell(cell)
        }

        // Add rows
        val sortedReports = reports.sortedBy { it.timestamp }
        sortedReports.forEachIndexed { index, report ->
            val parsed = parseResults(report.resultsJson)
            val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val bgColor = if (index % 2 == 0) DeviceRgb(255, 255, 255) else DeviceRgb(250, 250, 250)

            activeColumns.forEach { col ->
                when (col) {
                    ExportColumn.SOCKET -> table.addCell(helper.createDataCell(report.socketName ?: "-", bgColor, TextAlignment.CENTER))
                    ExportColumn.DATE -> table.addCell(helper.createDataCell(df.format(Date(report.timestamp)), bgColor, TextAlignment.CENTER))
                    ExportColumn.STATUS -> table.addCell(helper.createStatusCell(report.overallStatus))
                    ExportColumn.LINK_SPEED -> {
                        val rate = parsed?.link?.get("rate") ?: parsed?.link?.get("speed")
                        table.addCell(helper.createDataCell(normalizeLinkSpeed(rate), bgColor, TextAlignment.CENTER))
                    }
                    ExportColumn.NEIGHBOR -> {
                        val neighbor = parsed?.lldp?.firstOrNull()?.let { n ->
                            listOfNotNull(n.identity, n.interfaceName).joinToString(" / ")
                        } ?: "-"
                        table.addCell(helper.createDataCell(neighbor, bgColor, TextAlignment.CENTER))
                    }
                    ExportColumn.PING -> {
                        val pingValue = parsed?.ping?.lastOrNull()?.let { p ->
                            buildString {
                                if (!p.avgRtt.isNullOrBlank()) append("avg ${p.avgRtt}")
                                if (!p.packetLoss.isNullOrBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("loss ${p.packetLoss}%")
                                }
                            }
                        } ?: "-"
                        table.addCell(helper.createDataCell(pingValue, bgColor, TextAlignment.CENTER))
                    }
                    ExportColumn.TDR -> {
                        val tdrValue = parsed?.tdr?.firstOrNull()?.status ?: "-"
                        table.addCell(helper.createDataCell(tdrValue, bgColor, TextAlignment.CENTER))
                    }
                    ExportColumn.SPEED_TEST -> {
                        val speedValue = parsed?.speedTest?.let { st ->
                            val down = cleanCpuStrings(st.tcpDownload)
                            val up = cleanCpuStrings(st.tcpUpload)
                            listOfNotNull(
                                if (down.isNotBlank()) down else null,
                                if (up.isNotBlank()) up else null
                            ).joinToString(" / ")
                        } ?: "-"
                        table.addCell(helper.createDataCell(speedValue, bgColor, TextAlignment.CENTER))
                    }
                }
            }

            // Flush every 50 rows
            if (table.numberOfRows % 50 == 0) {
                table.flush()
            }
        }

        document.add(table)
    }

    private fun hasDataForColumn(report: Report, col: ExportColumn): Boolean {
        // Socket, Date, Status always have data (or are metadata)
        if (col == ExportColumn.SOCKET || col == ExportColumn.DATE || col == ExportColumn.STATUS) return true
        
        val parsed = parseResults(report.resultsJson) ?: return false
        
        return when (col) {
            ExportColumn.LINK_SPEED -> {
                val rate = parsed.link?.get("rate") ?: parsed.link?.get("speed")
                !rate.isNullOrBlank()
            }
            ExportColumn.NEIGHBOR -> !parsed.lldp.isNullOrEmpty()
            ExportColumn.PING -> !parsed.ping.isNullOrEmpty()
            ExportColumn.TDR -> !parsed.tdr.isNullOrEmpty()
            ExportColumn.SPEED_TEST -> parsed.speedTest != null && 
                (!parsed.speedTest.tcpDownload.isNullOrBlank() || !parsed.speedTest.tcpUpload.isNullOrBlank())
            else -> true
        }
    }


    
    // Removed old scanColumnsAndCpu, buildColumnWidths, addTableHeader, addTableRow relying on Flags - replaced by config driven ones above.
    // Helper used by createDataCell is kept below.

    private fun cleanCpuStrings(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val patterns = listOf(
            Pattern.compile("local-cpu-load:[^,)<]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("remote-cpu-load:[^,)<]+", Pattern.CASE_INSENSITIVE)
        )
        var out = input
        patterns.forEach { p -> out = p.matcher(out ?: "").replaceAll("") }
        return out?.replace(Regex("[ ]{2,}"), " ")?.trim()?.trim(',') ?: ""
    }

    // Reuse parseResults from original PdfGenerator
    internal fun parseResults(json: String): ParsedResults? {
        return parsedResultsParser.parse(json)
    }
}

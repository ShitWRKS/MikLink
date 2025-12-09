package com.app.miklink.data.pdf

import android.content.Context
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.ui.history.model.ParsedResults
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
) {

    // instantiate a parser helper internally to avoid changing constructor signature that
    // annotation processors (KSP/Hilt) may attempt to resolve during processing.
    private val parsedResultsParser: ParsedResultsParser = ParsedResultsParser(moshi)


    private data class ColumnFlags(
        val ping: Boolean,
        val tdr: Boolean,
        val speed: Boolean,
        val neighbor: Boolean,
        val showCpuWarning: Boolean
    )
    
    /**
     * Event handler for adding page numbers to each page
     */
    private class PageNumberEventHandler : com.itextpdf.kernel.events.IEventHandler {
        override fun handleEvent(event: com.itextpdf.kernel.events.Event) {
            val docEvent = event as com.itextpdf.kernel.events.PdfDocumentEvent
            val pdfDoc = docEvent.document
            val page = docEvent.page
            val pageNumber = pdfDoc.getPageNumber(page)
            val pageSize = page.pageSize
            
            val canvas = com.itextpdf.kernel.pdf.canvas.PdfCanvas(page)
            canvas.beginText()
            try {
                canvas.setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA), 9f)
                canvas.setColor(DeviceRgb(153, 153, 153), true)
                canvas.moveText((pageSize.right - 60).toDouble(), (pageSize.bottom + 20).toDouble())
                canvas.showText("Pag. $pageNumber")
            } finally {
                canvas.endText()
                canvas.release()
            }
        }
    }


    /**
     * Generate PDF report from list of reports and client data.
     * Returns a File object pointing to the generated PDF in cache directory.
     */
    fun generatePdfReport(
        reports: List<Report>,
        client: Client?,
        reportTitle: String
    ): File {
        val outputFile = File(context.cacheDir, "$reportTitle.pdf")
        val writer = PdfWriter(FileOutputStream(outputFile))
        val pdfDoc = PdfDocument(writer)
        
        // Add page event handler for page numbers
        pdfDoc.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE, PageNumberEventHandler())
        
        val document = Document(pdfDoc, PageSize.A4)

        // Set document margins (extra bottom for footer)
        document.setMargins(40f, 40f, 60f, 40f)

        // Add document sections
        addDocumentHeader(document, client, reports.firstOrNull())
        addClientInfoRow(document, client)
        addSummaryStats(document, reports)
        addResultsTable(document, reports)
        addFooter(document, client, reports)

        document.close()
        return outputFile
    }

    private fun addDocumentHeader(document: Document, client: Client?, firstReport: Report?) {
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Header table: Title on left, Doc info on right
        val headerTable = Table(floatArrayOf(3f, 2f))
            .setWidth(UnitValue.createPercentValue(100f))
        
        // Left cell: Title and subtitle
        val leftCell = Cell()
            .add(Paragraph("Collaudo Rete")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(22f)
                .setMarginBottom(2f))
            .add(Paragraph("Report di collaudo infrastruttura")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(12f)
                .setFontColor(DeviceRgb(102, 102, 102)))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
        
        // Right cell: Doc info
        val rightCell = Cell()
            .add(Paragraph("DOC #${firstReport?.reportId ?: "N/A"}")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(10f)
                .setFontColor(DeviceRgb(153, 153, 153))
                .setTextAlignment(TextAlignment.RIGHT))
            .add(Paragraph(df.format(Date()))
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(10f)
                .setFontColor(DeviceRgb(153, 153, 153))
                .setTextAlignment(TextAlignment.RIGHT))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
        
        headerTable.addCell(leftCell)
        headerTable.addCell(rightCell)
        document.add(headerTable.setMarginBottom(12f))
        
        // Gray separator line
        val separator = com.itextpdf.layout.element.LineSeparator(
            com.itextpdf.kernel.pdf.canvas.draw.SolidLine().apply {
                color = DeviceRgb(200, 200, 200)
            }
        )
        separator.setMarginBottom(16f)
        document.add(separator)
    }

    private fun addClientInfoRow(document: Document, client: Client?) {
        // Client name prominently
        val clientPara = Paragraph()
            .add(com.itextpdf.layout.element.Text("Cliente: ")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(102, 102, 102)))
            .add(com.itextpdf.layout.element.Text(client?.companyName ?: "N/A")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(14f)
                .setFontColor(DeviceRgb(17, 17, 17)))
        
        // Additional info only if present
        val hasLocation = !client?.location.isNullOrBlank() && client?.location != "-"
        val hasNetworkMode = !client?.networkMode.isNullOrBlank()
        
        if (hasLocation) {
            clientPara.add(com.itextpdf.layout.element.Text("  •  Sede: ")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(102, 102, 102)))
            clientPara.add(com.itextpdf.layout.element.Text(client?.location ?: "")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(17, 17, 17)))
        }
        
        if (hasNetworkMode) {
            clientPara.add(com.itextpdf.layout.element.Text("  •  Modalità: ")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(102, 102, 102)))
            clientPara.add(com.itextpdf.layout.element.Text(client?.networkMode ?: "")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(17, 17, 17)))
        }
        
        clientPara.setMarginBottom(12f)
        document.add(clientPara)
        
        // Gray separator line
        val separator = com.itextpdf.layout.element.LineSeparator(
            com.itextpdf.kernel.pdf.canvas.draw.SolidLine().apply {
                color = DeviceRgb(200, 200, 200)
            }
        )
        separator.setMarginBottom(16f)
        document.add(separator)
    }

    private fun addSummaryStats(document: Document, reports: List<Report>) {
        val total = reports.size
        val passed = reports.count { it.overallStatus.equals("PASS", ignoreCase = true) }
        val failed = total - passed

        // Inline badges using a simple table
        val badgesTable = Table(floatArrayOf(1f, 0.2f, 1f, 0.2f, 1f, 2f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(16f)
        
        // Badge: Totali  
        badgesTable.addCell(Cell()
            .add(Paragraph("$total Totali")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(33, 150, 243))
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(DeviceRgb(227, 242, 253))
            .setBorder(SolidBorder(DeviceRgb(33, 150, 243), 1f))
            .setBorderRadius(com.itextpdf.layout.properties.BorderRadius(6f))
            .setPadding(8f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE))
        
        // Spacer
        badgesTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        
        // Badge: Superati
        badgesTable.addCell(Cell()
            .add(Paragraph("$passed Superati")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(46, 125, 50))
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(DeviceRgb(232, 245, 233))
            .setBorder(SolidBorder(DeviceRgb(76, 175, 80), 1f))
            .setBorderRadius(com.itextpdf.layout.properties.BorderRadius(6f))
            .setPadding(8f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE))
        
        // Spacer
        badgesTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        
        // Badge: Falliti
        badgesTable.addCell(Cell()
            .add(Paragraph("$failed Falliti")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(198, 40, 40))
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(DeviceRgb(255, 235, 238))
            .setBorder(SolidBorder(DeviceRgb(244, 67, 54), 1f))
            .setBorderRadius(com.itextpdf.layout.properties.BorderRadius(6f))
            .setPadding(8f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE))
        
        // Empty filler cell
        badgesTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        
        document.add(badgesTable)
        
        // Gray separator before table
        val separator = com.itextpdf.layout.element.LineSeparator(
            com.itextpdf.kernel.pdf.canvas.draw.SolidLine().apply {
                color = DeviceRgb(200, 200, 200)
            }
        )
        separator.setMarginBottom(16f)
        document.add(separator)
    }


    private fun addResultsTable(document: Document, reports: List<Report>) {
        // Section title
        document.add(Paragraph("📋 Dettaglio Test")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(10f)
            .setFontColor(DeviceRgb(102, 102, 102))
            .setMarginBottom(12f))

        // Analyze which columns to show
        val flags = scanColumnsAndCpu(reports)

        // Calculate column widths
        val columnWidths = buildColumnWidths(flags)
        val table = Table(UnitValue.createPercentArray(columnWidths))
            .setWidth(UnitValue.createPercentValue(100f))

        // Add header
        addTableHeader(table, flags)

        // Add rows
        val sortedReports = reports.sortedBy { it.timestamp }
        sortedReports.forEachIndexed { index, report ->
            addTableRow(table, report, flags, index)

            // Flush every 50 rows to optimize memory
            if (table.numberOfRows % 50 == 0) {
                table.flush()
            }
        }

        document.add(table)
    }

    private fun scanColumnsAndCpu(reports: List<Report>): ColumnFlags {
        var hasPing = false
        var hasTdr = false
        var hasSpeed = false
        var hasNeighbor = false
        var cpuWarn = false

        reports.forEach { r ->
            val parsed = parseResults(r.resultsJson)
            if (!hasPing) hasPing = (parsed?.ping?.isNotEmpty() == true)
            if (!hasTdr) hasTdr = (parsed?.tdr?.isNotEmpty() == true)
            if (!hasSpeed) hasSpeed = (parsed?.speedTest != null)
            
            // Check if neighbor (LLDP) has meaningful data
            if (!hasNeighbor) {
                val neighbor = parsed?.lldp?.firstOrNull()?.let { n ->
                    listOfNotNull(n.identity, n.interfaceName).joinToString(" / ")
                }
                hasNeighbor = !neighbor.isNullOrBlank() && neighbor != "-"
            }

            parsed?.speedTest?.let { st ->
                val probe = listOf(
                    st.status, st.ping, st.jitter, st.loss, st.tcpDownload, st.tcpUpload, st.udpDownload, st.udpUpload, st.warning
                ).joinToString(" ") { it ?: "" }
                if (!cpuWarn) {
                    cpuWarn = probe.contains("local-cpu-load:100%", ignoreCase = true) ||
                            probe.contains("remote-cpu-load:100%", ignoreCase = true)
                }
            }
        }
        return ColumnFlags(ping = hasPing, tdr = hasTdr, speed = hasSpeed, neighbor = hasNeighbor, showCpuWarning = cpuWarn)
    }

    private fun buildColumnWidths(flags: ColumnFlags): FloatArray {
        val widths = mutableListOf(12f, 18f, 12f) // Presa, Data/Ora, Stato
        if (flags.neighbor) widths.add(20f)  // Neighbor only if has data
        if (flags.ping) widths.add(15f)
        if (flags.tdr) widths.add(10f)
        if (flags.speed) widths.add(18f)
        return widths.toFloatArray()
    }

    private fun addTableHeader(table: Table, flags: ColumnFlags) {
        val headers = buildList {
            add("Presa")
            add("Data/Ora")
            add("Stato")
            if (flags.neighbor) add("Neighbor")  // Conditional
            if (flags.ping) add("Ping")
            if (flags.tdr) add("TDR")
            if (flags.speed) add("Speed Test")
        }

        headers.forEach { headerText ->
            val cell = Cell()
                .add(Paragraph(headerText)
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(DeviceRgb(241, 243, 244))
                .setBorder(SolidBorder(DeviceRgb(221, 221, 221), 1f))
                .setPadding(8f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
            table.addHeaderCell(cell)
        }
    }

    private fun addTableRow(
        table: Table,
        report: Report,
        flags: ColumnFlags,
        index: Int
    ) {
        val parsed = parseResults(report.resultsJson)
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Background color alternating
        val bgColor = if (index % 2 == 0) DeviceRgb(255, 255, 255) else DeviceRgb(250, 250, 250)

        // Presa (centered)
        table.addCell(createDataCell(report.socketName ?: "-", bgColor, TextAlignment.CENTER))

        // Data/Ora (left-aligned)
        table.addCell(createDataCell(df.format(Date(report.timestamp)), bgColor, TextAlignment.LEFT))

        // Stato (with colored badge)
        table.addCell(createStatusCell(report.overallStatus))

        // Neighbor (conditional - only if flags.neighbor is true)
        if (flags.neighbor) {
            val neighbor = parsed?.lldp?.firstOrNull()?.let { n ->
                listOfNotNull(n.identity, n.interfaceName).joinToString(" / ")
            } ?: "-"
            table.addCell(createDataCell(neighbor, bgColor, TextAlignment.CENTER))
        }

        // Dynamic columns
        if (flags.ping) {
            val pingValue = parsed?.ping?.lastOrNull()?.let { p ->
                buildString {
                    if (!p.avgRtt.isNullOrBlank()) append("avg ${p.avgRtt}")
                    if (!p.packetLoss.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append("loss ${p.packetLoss}%")
                    }
                }
            } ?: "-"
            table.addCell(createDataCell(pingValue, bgColor, TextAlignment.CENTER))
        }

        if (flags.tdr) {
            val tdrValue = parsed?.tdr?.firstOrNull()?.status ?: "-"
            table.addCell(createDataCell(tdrValue, bgColor, TextAlignment.CENTER))
        }

        if (flags.speed) {
            val speedValue = parsed?.speedTest?.let { st ->
                val down = cleanCpuStrings(st.tcpDownload)
                val up = cleanCpuStrings(st.tcpUpload)
                listOfNotNull(
                    if (down.isNotBlank()) down else null,
                    if (up.isNotBlank()) up else null
                ).joinToString(" / ")
            } ?: "-"
            table.addCell(createDataCell(speedValue, bgColor, TextAlignment.CENTER))
        }
    }

    private fun createDataCell(content: String, bgColor: DeviceRgb, alignment: TextAlignment = TextAlignment.CENTER): Cell {
        return Cell()
            .add(Paragraph(content)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(10f)
                .setFontColor(DeviceRgb(51, 51, 51))
                .setTextAlignment(alignment))
            .setBackgroundColor(bgColor)
            .setBorder(SolidBorder(DeviceRgb(238, 238, 238), 0.5f))
            .setPadding(10f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }

    private fun createStatusCell(status: String): Cell {
        val isPassed = status.equals("PASS", ignoreCase = true)

        return Cell()
            .add(Paragraph(status.uppercase())
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(9f)
                .setFontColor(
                    if (isPassed) DeviceRgb(46, 125, 50)
                    else DeviceRgb(198, 40, 40)
                ))
            .setBackgroundColor(
                if (isPassed) DeviceRgb(232, 245, 233)
                else DeviceRgb(255, 235, 238)
            )
            .setBorder(SolidBorder(DeviceRgb(238, 238, 238), 0.5f))
            .setPadding(6f)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }

    private fun addFooter(document: Document, client: Client?, reports: List<Report>) {
        val flags = scanColumnsAndCpu(reports)

        // CPU Warning if needed
        if (flags.showCpuWarning) {
            val warning = Paragraph("⚠️ Rilevato carico CPU 100% (locale/remota) durante alcuni Speed Test. I valori potrebbero essere sottostimati.")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(230, 81, 0))
                .setBackgroundColor(DeviceRgb(255, 248, 225))
                .setBorder(SolidBorder(DeviceRgb(255, 213, 79), 1f))
                .setPadding(10f)
                .setMarginTop(12f)
                .setMarginBottom(12f)
            document.add(warning)
        }

        // Client notes if present
        if (!client?.notes.isNullOrBlank()) {
            val notesSection = Paragraph()
            notesSection.add(Paragraph("Note Cliente")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(9f)
                .setFontColor(DeviceRgb(102, 102, 102))
                .setMarginBottom(4f))
            notesSection.add(Paragraph(client.notes ?: "")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(68, 68, 68)))

            val notesCell = Cell()
                .add(notesSection)
                .setBackgroundColor(DeviceRgb(248, 249, 250))
                .setBorder(SolidBorder(DeviceRgb(224, 224, 224), 1f))
                .setPadding(10f)
                .setMarginTop(12f)
                .setMarginBottom(12f)

            val notesTable = Table(floatArrayOf(1f))
                .setWidth(UnitValue.createPercentValue(100f))
                .addCell(notesCell)
            document.add(notesTable)
        }

        // Generation timestamp
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val footer = Paragraph()
            .add("Generato il ${df.format(Date())}  •  MikLink • Certificazione Reti")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
            .setFontSize(10f)
            .setFontColor(DeviceRgb(153, 153, 153))
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(16f)
        document.add(footer)
    }

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

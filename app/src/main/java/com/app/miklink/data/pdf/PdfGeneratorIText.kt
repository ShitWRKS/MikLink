package com.app.miklink.data.pdf

import android.content.Context
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.db.model.TestProfile
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
) {


    private data class ColumnFlags(
        val ping: Boolean,
        val tdr: Boolean,
        val speed: Boolean,
        val linkSpeed: Boolean,
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

    /**
     * Generate detailed PDF for a single test report.
     * Includes Hero section, conditional Network Info, Test Results, Profile settings.
     */
    fun generateSingleTestPdf(
        report: Report,
        client: Client?,
        profile: TestProfile?,
        reportTitle: String
    ): File {
        val outputFile = File(context.cacheDir, "$reportTitle.pdf")
        val writer = PdfWriter(FileOutputStream(outputFile))
        val pdfDoc = PdfDocument(writer)
        
        // Add page event handler for page numbers
        pdfDoc.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE, PageNumberEventHandler())
        
        val document = Document(pdfDoc, PageSize.A4)
        document.setMargins(30f, 30f, 50f, 30f)
        
        val parsed = parseResults(report.resultsJson)
        
        // Document sections
        addSingleTestDocumentHeader(document, client, report)
        addSingleTestHeroSection(document, report, parsed)
        addNetworkInfoSection(document, parsed)
        addTestResultsDetailSection(document, parsed)
        addTestProfileSection(document, profile)
        addSingleTestNotesSection(document, report)
        addSingleTestFooter(document, client)
        
        document.close()
        return outputFile
    }

    private fun addSingleTestDocumentHeader(document: Document, client: Client?, report: Report) {
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Title: Single Test Report
        val titlePara = Paragraph("Report di Collaudo - Singola Presa")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(18f)
            .setFontColor(DeviceRgb(17, 17, 17))
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(8f)
        document.add(titlePara)
        
        // Client and Socket prominence
        val clientSocketTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(4f)
        
        // Client name (left)
        clientSocketTable.addCell(Cell()
            .add(Paragraph()
                .add(com.itextpdf.layout.element.Text("Cliente: ")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                    .setFontSize(10f)
                    .setFontColor(DeviceRgb(102, 102, 102)))
                .add(com.itextpdf.layout.element.Text(client?.companyName ?: "N/A")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(12f)
                    .setFontColor(DeviceRgb(17, 17, 17))))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setTextAlignment(TextAlignment.LEFT))
        
        // Socket name (right)
        clientSocketTable.addCell(Cell()
            .add(Paragraph()
                .add(com.itextpdf.layout.element.Text("Presa: ")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                    .setFontSize(10f)
                    .setFontColor(DeviceRgb(102, 102, 102)))
                .add(com.itextpdf.layout.element.Text(report.socketName ?: "N/A")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(12f)
                    .setFontColor(DeviceRgb(17, 17, 17))))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT))
        
        document.add(clientSocketTable)
        
        // Doc info and date
        val infoTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(12f)
        
        infoTable.addCell(Cell()
            .add(Paragraph("DOC #${report.reportId}")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(9f)
                .setFontColor(DeviceRgb(153, 153, 153)))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setTextAlignment(TextAlignment.LEFT))
        
        infoTable.addCell(Cell()
            .add(Paragraph(df.format(Date()))
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(9f)
                .setFontColor(DeviceRgb(153, 153, 153)))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT))
        
        document.add(infoTable)
        
        // Separator
        addSeparator(document)
    }

    private fun addSingleTestHeroSection(document: Document, report: Report, parsed: ParsedResults?) {
        val isPassed = report.overallStatus.equals("PASS", ignoreCase = true)
        
        // Large status badge centered - SQUARE design with consistent border
        val badgeTable = Table(1).setWidth(UnitValue.createPercentValue(100f))
        val badgeCell = Cell()
            .add(Paragraph(report.overallStatus.uppercase())
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(24f)
                .setFontColor(if (isPassed) DeviceRgb(46, 125, 50) else DeviceRgb(198, 40, 40)))
            .setBackgroundColor(if (isPassed) DeviceRgb(232, 245, 233) else DeviceRgb(255, 235, 238))
            .setBorder(SolidBorder(if (isPassed) DeviceRgb(76, 175, 80) else DeviceRgb(244, 67, 54), 2f))
            .setPadding(12f)
            .setTextAlignment(TextAlignment.CENTER)
        badgeTable.addCell(badgeCell)
        document.add(badgeTable.setMarginBottom(12f))
        
        addSeparator(document)
    }

    private fun addNetworkInfoSection(document: Document, parsed: ParsedResults?) {
        val hasLink = parsed?.link != null && parsed.link.isNotEmpty()
        val hasLldp = parsed?.lldp?.isNotEmpty() == true
        
        if (!hasLink && !hasLldp) return
        
        document.add(Paragraph("📡 Network Information")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(11f)
            .setMarginBottom(6f))
        
        val table = Table(2).setWidth(UnitValue.createPercentValue(100f))
        
        // Link info
        if (hasLink) {
            parsed?.link?.forEach { (key, value) ->
                if (value.isNotBlank() && value != "-") {
                    val normalizedValue = when(key.lowercase()) {
                        "status" -> normalizeLinkStatus(value)
                        "speed", "rate" -> normalizeLinkSpeed(value)
                        else -> value
                    }
                    table.addCell(createInfoLabelCell(key))
                    table.addCell(createInfoValueCell(normalizedValue))
                }
            }
        }
        
        // LLDP info
        if (hasLldp) {
            parsed?.lldp?.forEachIndexed { index, neighbor ->
                if (index > 0) {
                    // Separator between neighbors
                    table.addCell(Cell(1, 2)
                        .add(Paragraph("───").setTextAlignment(TextAlignment.CENTER).setFontSize(8f))
                        .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                        .setPadding(4f))
                }
                
                if (!neighbor.identity.isNullOrBlank()) {
                    table.addCell(createInfoLabelCell("Neighbor"))
                    table.addCell(createInfoValueCell(neighbor.identity!!))
                }
                if (!neighbor.interfaceName.isNullOrBlank()) {
                    table.addCell(createInfoLabelCell("Interface"))
                    table.addCell(createInfoValueCell(neighbor.interfaceName!!))
                }
                if (!neighbor.systemCaps.isNullOrBlank()) {
                    table.addCell(createInfoLabelCell("Capabilities"))
                    table.addCell(createInfoValueCell(neighbor.systemCaps!!))
                }
                neighbor.vlanId?.let {
                    if (it.isNotBlank()) {
                        table.addCell(createInfoLabelCell("VLAN"))
                        table.addCell(createInfoValueCell(it))
                    }
                }
            }
        }
        
        document.add(table.setMarginBottom(16f))
        addSeparator(document)
    }

    private fun addTestResultsDetailSection(document: Document, parsed: ParsedResults?) {
        val hasPing = parsed?.ping?.isNotEmpty() == true
        val hasTdr = parsed?.tdr?.isNotEmpty() == true
        
        if (!hasPing && !hasTdr) return
        
        document.add(Paragraph("📊 Test Results")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(11f)
            .setMarginBottom(6f))
        
        val table = Table(2).setWidth(UnitValue.createPercentValue(100f))
        
        // Ping results - CONDENSED
        if (hasPing) {
            val summary = parsed?.ping?.lastOrNull()
            table.addCell(createSectionHeaderCell("Ping Test", 2))
            
            // RTT in one line: Min / Avg / Max
            if (!summary?.minRtt.isNullOrBlank() || !summary?.avgRtt.isNullOrBlank() || !summary?.maxRtt.isNullOrBlank()) {
                table.addCell(createInfoLabelCell("RTT"))
                val rttParts = listOfNotNull(
                    summary?.minRtt?.let { "min ${normalizeTime(it)}" },
                    summary?.avgRtt?.let { "avg ${normalizeTime(it)}" },
                    summary?.maxRtt?.let { "max ${normalizeTime(it)}" }
                ).joinToString(" / ")
                table.addCell(createInfoValueCell(rttParts))
            }
            
            // Packet Loss
            if (!summary?.packetLoss.isNullOrBlank()) {
                table.addCell(createInfoLabelCell("Packet Loss"))
                table.addCell(createInfoValueCell("${summary?.packetLoss}%"))
            }
        }
        
        // TDR results
        if (hasTdr) {
            if (hasPing) {
                table.addCell(Cell(1, 2)
                    .add(Paragraph("───").setTextAlignment(TextAlignment.CENTER).setFontSize(8f))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(2f))
            }
            
            table.addCell(createSectionHeaderCell("Cable Test (TDR)", 2))
            parsed?.tdr?.forEach { tdr ->
                table.addCell(createInfoLabelCell("Status"))
                val statusCell = Cell()
                    .add(Paragraph(tdr.status)
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                        .setFontSize(10f)
                        .setFontColor(
                            if (tdr.status.contains("ok", ignoreCase = true))
                                DeviceRgb(46, 125, 50)
                            else
                                DeviceRgb(198, 40, 40)
                        ))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(6f)
                table.addCell(statusCell)
            }
        }
        
        document.add(table.setMarginBottom(10f))
        addSeparator(document)
    }

    private fun addTestProfileSection(document: Document, profile: TestProfile?) {
        if (profile == null) return
        
        document.add(Paragraph("⚙️ Test Configuration")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(11f)
            .setMarginBottom(6f))
        
        val table = Table(2).setWidth(UnitValue.createPercentValue(100f))
        
        table.addCell(createInfoLabelCell("Profile"))
        table.addCell(createInfoValueCell(profile.profileName))
        
        if (!profile.profileDescription.isNullOrBlank()) {
            table.addCell(createInfoLabelCell("Description"))
            table.addCell(createInfoValueCell(profile.profileDescription!!))
        }
        
        // Tests enabled
        val testsEnabled = buildList {
            if (profile.runTdr) add("TDR")
            if (profile.runLinkStatus) add("Link Status")
            if (profile.runLldp) add("LLDP")
            if (profile.runPing) add("Ping")
            if (profile.runSpeedTest) add("Speed Test")
        }.joinToString(", ")
        
        if (testsEnabled.isNotBlank()) {
            table.addCell(createInfoLabelCell("Tests Enabled"))
            table.addCell(createInfoValueCell(testsEnabled))
        }
        
        // Ping configuration
        if (profile.runPing) {
            val targets = listOfNotNull(
                profile.pingTarget1,
                profile.pingTarget2,
                profile.pingTarget3
            ).filter { it.isNotBlank() }.joinToString(", ")
            
            if (targets.isNotBlank()) {
                table.addCell(createInfoLabelCell("Ping Targets"))
                table.addCell(createInfoValueCell(targets))
                table.addCell(createInfoLabelCell("Ping Count"))
                table.addCell(createInfoValueCell(profile.pingCount.toString()))
            }
        }
        
        document.add(table.setMarginBottom(16f))
        addSeparator(document)
    }

    private fun addProbeInfoSection(document: Document, parsed: ParsedResults?, report: Report) {
        val lldpInfo = parsed?.lldp?.firstOrNull()
        val hasProbeInfo = !report.probeName.isNullOrBlank() || lldpInfo != null
        
        if (!hasProbeInfo) return
        
        document.add(Paragraph("🔧 Probe Information")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(12f)
            .setMarginBottom(8f))
        
        val table = Table(2).setWidth(UnitValue.createPercentValue(100f))
        
        if (!report.probeName.isNullOrBlank()) {
            table.addCell(createInfoLabelCell("Probe Name"))
            table.addCell(createInfoValueCell(report.probeName!!))
        }
        
        lldpInfo?.let { info ->
            if (!info.systemDescription.isNullOrBlank()) {
                table.addCell(createInfoLabelCell("Model"))
                table.addCell(createInfoValueCell(info.systemDescription!!))
            }
            if (!info.portId.isNullOrBlank() || !info.interfaceName.isNullOrBlank()) {
                table.addCell(createInfoLabelCell("Interface"))
                table.addCell(createInfoValueCell(info.portId ?: info.interfaceName ?: "-"))
            }
        }
        
        document.add(table.setMarginBottom(16f))
        addSeparator(document)
    }

    private fun addSingleTestNotesSection(document: Document, report: Report) {
        if (report.notes.isNullOrBlank()) return
        
        document.add(Paragraph("📝 Notes")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(11f)
            .setMarginBottom(6f))
        
        val notesCell = Cell()
            .add(Paragraph(report.notes!!)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(68, 68, 68)))
            .setBackgroundColor(DeviceRgb(248, 249, 250))
            .setBorder(SolidBorder(DeviceRgb(224, 224, 224), 1f))
            .setPadding(10f)
        
        val notesTable = Table(1).setWidth(UnitValue.createPercentValue(100f)).addCell(notesCell)
        document.add(notesTable.setMarginBottom(8f))
    }

    private fun addSingleTestFooter(document: Document, client: Client?) {
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

    // Helper methods for consistent styling
    private fun createInfoLabelCell(label: String): Cell {
        return Cell()
            .add(Paragraph(label)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(10f)
                .setFontColor(DeviceRgb(102, 102, 102)))
            .setBackgroundColor(DeviceRgb(250, 250, 250))
            .setBorder(SolidBorder(DeviceRgb(238, 238, 238), 0.5f))
            .setPadding(8f)
    }

    private fun createInfoValueCell(value: String): Cell {
        return Cell()
            .add(Paragraph(value)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(10f)
                .setFontColor(DeviceRgb(17, 17, 17)))
            .setBorder(SolidBorder(DeviceRgb(238, 238, 238), 0.5f))
            .setPadding(8f)
    }

    private fun createSectionHeaderCell(title: String, colspan: Int): Cell {
        return Cell(1, colspan)
            .add(Paragraph(title)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(11f)
                .setFontColor(DeviceRgb(33, 150, 243)))
            .setBackgroundColor(DeviceRgb(227, 242, 253))
            .setBorder(SolidBorder(DeviceRgb(33, 150, 243), 1f))
            .setPadding(6f)
    }

    private fun addSeparator(document: Document) {
        val separator = com.itextpdf.layout.element.LineSeparator(
            com.itextpdf.kernel.pdf.canvas.draw.SolidLine().apply {
                color = DeviceRgb(200, 200, 200)
            }
        )
        separator.setMarginBottom(10f)
        document.add(separator)
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
        var hasLinkSpeed = false
        var hasNeighbor = false
        var cpuWarn = false

        reports.forEach { r ->
            val parsed = parseResults(r.resultsJson)
            if (!hasPing) hasPing = (parsed?.ping?.isNotEmpty() == true)
            if (!hasTdr) hasTdr = (parsed?.tdr?.isNotEmpty() == true)
            if (!hasSpeed) hasSpeed = (parsed?.speedTest != null)

            // Check link speed availability
            if (!hasLinkSpeed) {
                val rate = parsed?.link?.get("rate") ?: parsed?.link?.get("speed")
                hasLinkSpeed = !rate.isNullOrBlank() && rate != "-"
            }
            
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
        return ColumnFlags(ping = hasPing, tdr = hasTdr, speed = hasSpeed, linkSpeed = hasLinkSpeed, neighbor = hasNeighbor, showCpuWarning = cpuWarn)
    }

    private fun buildColumnWidths(flags: ColumnFlags): FloatArray {
        val widths = mutableListOf(12f, 18f, 12f) // Presa, Data/Ora, Stato
        if (flags.linkSpeed) widths.add(12f) // Link Speed
        if (flags.neighbor) widths.add(18f)  // Neighbor only if has data (reduced slightly to fit)
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
            if (flags.linkSpeed) add("Link Speed")
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

        // Link Speed
        if (flags.linkSpeed) {
            val rate = parsed?.link?.get("rate") ?: parsed?.link?.get("speed")
            val displayRate = normalizeLinkSpeed(rate)
            table.addCell(createDataCell(displayRate, bgColor, TextAlignment.CENTER))
        }

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
        if (json.isBlank()) return null

        var parsed: ParsedResults? = null
        try {
            parsed = moshi.adapter(ParsedResults::class.java).fromJson(json)
            if (parsed != null && parsed.ping != null && parsed.tdr != null) return parsed
        } catch (_: Exception) {
            // Ignore: will use legacy normalization
        }

        return try {
            val mapType = com.squareup.moshi.Types.newParameterizedType(
                Map::class.java, String::class.java, Any::class.java
            )
            val mapAdapter: com.squareup.moshi.JsonAdapter<Map<String, Any?>> = moshi.adapter(mapType)
            val root = mapAdapter.fromJson(json)

            if (root == null) return parsed

            var pingList: MutableList<com.app.miklink.data.network.PingResult>? = null

            if (parsed?.ping.isNullOrEmpty()) {
                val listType = com.squareup.moshi.Types.newParameterizedType(
                    List::class.java, com.app.miklink.data.network.PingResult::class.java
                )
                val pingListAdapter: com.squareup.moshi.JsonAdapter<List<com.app.miklink.data.network.PingResult>> = moshi.adapter(listType)
                root.forEach { (key, value) ->
                    if (key.startsWith("ping_")) {
                        var items = pingListAdapter.fromJsonValue(value) ?: emptyList()
                        if (items.isEmpty()) {
                            val rawList = (value as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
                            if (rawList.isNotEmpty()) {
                                items = rawList.map { m ->
                                    com.app.miklink.data.network.PingResult(
                                        avgRtt = m["avg-rtt"] as? String,
                                        host = m["host"] as? String,
                                        maxRtt = m["max-rtt"] as? String,
                                        minRtt = m["min-rtt"] as? String,
                                        packetLoss = m["packet-loss"] as? String,
                                        received = m["received"] as? String,
                                        sent = m["sent"] as? String,
                                        seq = m["seq"] as? String,
                                        size = m["size"] as? String,
                                        time = m["time"] as? String,
                                        ttl = m["ttl"] as? String
                                    )
                                }
                            }
                        }
                        if (items.isNotEmpty()) {
                            if (pingList == null) pingList = mutableListOf()
                            pingList!!.addAll(items)
                        }
                    }
                }
            }

            var tdrList: List<com.app.miklink.data.network.CableTestResult>? = parsed?.tdr
            if (tdrList == null) {
                val tdrVal = root["tdr"]
                when (tdrVal) {
                    is Map<*, *> -> {
                        val tdrAdapter: com.squareup.moshi.JsonAdapter<com.app.miklink.data.network.CableTestResult> =
                            moshi.adapter(com.app.miklink.data.network.CableTestResult::class.java)
                        val single = tdrAdapter.fromJsonValue(tdrVal)
                        if (single != null) tdrList = listOf(single)
                    }
                    is List<*> -> {
                        val listType = com.squareup.moshi.Types.newParameterizedType(
                            List::class.java, com.app.miklink.data.network.CableTestResult::class.java
                        )
                        val listAdapter: com.squareup.moshi.JsonAdapter<List<com.app.miklink.data.network.CableTestResult>> = moshi.adapter(listType)
                        tdrList = listAdapter.fromJsonValue(tdrVal)
                    }
                }
            }

            if (pingList == null && tdrList == null) parsed else ParsedResults(
                tdr = tdrList ?: parsed?.tdr,
                link = parsed?.link,
                lldp = parsed?.lldp,
                ping = pingList ?: parsed?.ping,
                speedTest = parsed?.speedTest
            )
        } catch (e: Exception) {
            android.util.Log.e("PdfGeneratorIText", "Error parsing results JSON (legacy)", e)
            parsed
        }
    }
}

package com.app.miklink.data.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.ui.history.model.ParsedResults
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {

    private companion object {
        const val A4_WIDTH_PT = 595
        const val A4_HEIGHT_PT = 842
        const val MARGIN = 40f
        const val LINE_HEIGHT = 14f
        const val SECTION_SPACING = 20f
        const val PAGE_BREAK_THRESHOLD = A4_HEIGHT_PT - 80f
    }

    // Paint configurations for professional styling
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#004D40")
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val headerPaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 10f
        isAntiAlias = true
    }

    private val smallTextPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 9f
        isAntiAlias = true
    }

    private val successPaint = Paint().apply {
        color = Color.parseColor("#2E7D32")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val failPaint = Paint().apply {
        color = Color.parseColor("#C62828")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    // Main entry point for single report PDF
    suspend fun createPdfFromReport(report: Report, client: Client?, outputUri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val parsedResults = parseResults(report.resultsJson)
                val document = PdfDocument()
                val pages = mutableListOf<PdfDocument.Page>()
                var pageNumber = 1
                var currentY = MARGIN

                // Start first page
                var currentPage = document.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create()
                )
                pages.add(currentPage)

                // Render header
                currentY = drawHeader(currentPage.canvas, currentY, "MikLink Test Report")
                currentY += SECTION_SPACING

                // Render metadata
                currentY = drawMetadataSection(currentPage.canvas, currentY, report, client)
                currentY += SECTION_SPACING

                // Check page break
                if (currentY > PAGE_BREAK_THRESHOLD) {
                    document.finishPage(currentPage)
                    pageNumber++
                    currentPage = document.startPage(
                        PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create()
                    )
                    pages.add(currentPage)
                    currentY = MARGIN
                }

                // Render status
                currentY = drawStatusSection(currentPage.canvas, currentY, report.overallStatus)
                currentY += SECTION_SPACING

                // Render test results
                if (parsedResults != null) {
                    val (newPage, newY) = drawTestResultsSection(
                        document, currentPage, currentY, parsedResults, pageNumber
                    )
                    currentPage = newPage
                    currentY = newY
                    if (pages.last() != currentPage) pages.add(currentPage)
                }

                // Render notes
                if (!report.notes.isNullOrBlank()) {
                    if (currentY > PAGE_BREAK_THRESHOLD - 100) {
                        document.finishPage(currentPage)
                        pageNumber++
                        currentPage = document.startPage(
                            PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create()
                        )
                        pages.add(currentPage)
                        currentY = MARGIN
                    }
                    currentY = drawNotesSection(currentPage.canvas, currentY, report.notes)
                }

                // Draw footer on all pages
                pages.forEachIndexed { index, page ->
                    drawFooter(page.canvas, index + 1, pages.size)
                }

                // Finish last page and write document
                document.finishPage(currentPage)
                context.contentResolver.openFileDescriptor(outputUri, "w")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use {
                        document.writeTo(it)
                    }
                }
                document.close()
                android.util.Log.d("PdfGenerator", "PDF created: ${pages.size} pages for report ${report.reportId}")
            } catch (e: Exception) {
                android.util.Log.e("PdfGenerator", "Error creating PDF", e)
                throw e
            }
        }
    }

    // Batch export for multiple reports
    suspend fun createBatchPdf(reports: List<Report>, client: Client?, outputUri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val document = PdfDocument()
                var pageNumber = 1

                // Create index page
                val indexPage = document.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create()
                )
                drawBatchIndexPage(indexPage.canvas, reports, client)
                drawFooter(indexPage.canvas, pageNumber, reports.size + 1) // +1 for index
                document.finishPage(indexPage)
                pageNumber++

                // Create page for each report
                reports.forEach { report ->
                    val parsedResults = parseResults(report.resultsJson)
                    val reportPage = document.startPage(
                        PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create()
                    )

                    var yPos = drawHeader(reportPage.canvas, MARGIN, "Test Report: ${report.socketName ?: "Unnamed"}")
                    yPos += 10f
                    yPos = drawMetadataSection(reportPage.canvas, yPos, report, client)
                    yPos += 10f
                    yPos = drawStatusSection(reportPage.canvas, yPos, report.overallStatus)
                    yPos += 10f

                    if (parsedResults != null && yPos < PAGE_BREAK_THRESHOLD - 50) {
                        drawTestResultsSummary(reportPage.canvas, yPos, parsedResults)
                    }

                    drawFooter(reportPage.canvas, pageNumber, reports.size + 1)
                    document.finishPage(reportPage)
                    pageNumber++
                }

                context.contentResolver.openFileDescriptor(outputUri, "w")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use {
                        document.writeTo(it)
                    }
                }
                document.close()
                android.util.Log.d("PdfGenerator", "Batch PDF created: ${reports.size} reports")
            } catch (e: Exception) {
                android.util.Log.e("PdfGenerator", "Error creating batch PDF", e)
                throw e
            }
        }
    }

    /**
     * `internal` per test unitari.
     * Parsa il JSON dei risultati del report in un oggetto strutturato.
     */
    internal fun parseResults(json: String): ParsedResults? {
        return try {
            moshi.adapter(ParsedResults::class.java).fromJson(json)
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Error parsing results JSON", e)
            null
        }
    }

    private fun drawHeader(canvas: Canvas, y: Float, title: String): Float {
        // Draw colored header box
        val headerBox = RectF(MARGIN, y, A4_WIDTH_PT - MARGIN, y + 35f)
        val boxPaint = Paint().apply {
            color = Color.parseColor("#E0F2F1")
            style = Paint.Style.FILL
        }
        canvas.drawRect(headerBox, boxPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#004D40")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(headerBox, borderPaint)

        canvas.drawText(title, MARGIN + 10f, y + 24f, titlePaint)
        return y + 40f
    }

    private fun drawMetadataSection(canvas: Canvas, y: Float, report: Report, client: Client?): Float {
        var yPos = y
        canvas.drawText("Report Information", MARGIN, yPos, headerPaint)
        yPos += LINE_HEIGHT + 6f
        drawSeparator(canvas, yPos)
        yPos += 6f

        val col1X = MARGIN + 10f
        val col2X = A4_WIDTH_PT / 2 + 10f
        val labelWidth = 90f

        // Column 1
        canvas.drawText("Client:", col1X, yPos, textPaint)
        canvas.drawText(client?.companyName ?: "N/A", col1X + labelWidth, yPos, textPaint)
        yPos += LINE_HEIGHT

        canvas.drawText("Socket ID:", col1X, yPos, textPaint)
        canvas.drawText(report.socketName ?: "N/A", col1X + labelWidth, yPos, textPaint)

        // Column 2
        var col2Y = y + LINE_HEIGHT + 12f
        canvas.drawText("Date/Time:", col2X, col2Y, textPaint)
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        canvas.drawText(dateStr, col2X + labelWidth, col2Y, textPaint)
        col2Y += LINE_HEIGHT


        yPos += LINE_HEIGHT + 4f

        // Additional info
        if (!report.probeName.isNullOrBlank()) {
            canvas.drawText("Probe:", col1X, yPos, textPaint)
            canvas.drawText(report.probeName, col1X + labelWidth, yPos, textPaint)
        }
        if (!report.profileName.isNullOrBlank()) {
            canvas.drawText("Profile:", col2X, yPos, textPaint)
            canvas.drawText(report.profileName, col2X + labelWidth, yPos, textPaint)
        }

        return yPos + LINE_HEIGHT + 4f
    }

    private fun drawStatusSection(canvas: Canvas, y: Float, status: String): Float {
        var yPos = y
        canvas.drawText("Overall Status", MARGIN, yPos, headerPaint)
        yPos += LINE_HEIGHT + 6f

        // Status box with icon
        val statusColor = if (status == "PASS") Color.parseColor("#C8E6C9") else Color.parseColor("#FFCDD2")
        val boxRect = RectF(MARGIN + 10f, yPos, MARGIN + 120f, yPos + 30f)
        val boxPaint = Paint().apply {
            color = statusColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(boxRect, boxPaint)

        val statusPaint = if (status == "PASS") successPaint else failPaint
        val icon = if (status == "PASS") "✓" else "✗"
        canvas.drawText("$icon $status", MARGIN + 20f, yPos + 20f, statusPaint)

        return yPos + 35f
    }

    private fun drawTestResultsSection(
        document: PdfDocument,
        currentPage: PdfDocument.Page,
        startY: Float,
        results: ParsedResults,
        startPageNumber: Int
    ): Pair<PdfDocument.Page, Float> {
        var page = currentPage
        var yPos = startY
        var pageNum = startPageNumber

        page.canvas.drawText("Test Results", MARGIN, yPos, headerPaint)
        yPos += LINE_HEIGHT + 6f
        drawSeparator(page.canvas, yPos)
        yPos += 8f

        // Link Status
        results.link?.let { linkData ->
            if (yPos > PAGE_BREAK_THRESHOLD) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNum).create()
                )
                yPos = MARGIN
            }
            yPos = drawLinkStatus(page.canvas, yPos, linkData)
        }

        // LLDP Neighbors
        results.lldp?.let { neighbors ->
            if (yPos > PAGE_BREAK_THRESHOLD) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNum).create()
                )
                yPos = MARGIN
            }
            yPos = drawLldpNeighbors(page.canvas, yPos, neighbors)
        }

        // Ping Results
        results.ping?.let { pings ->
            if (yPos > PAGE_BREAK_THRESHOLD - 50) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNum).create()
                )
                yPos = MARGIN
            }
            yPos = drawPingResults(page.canvas, yPos, pings)
        }

        // TDR Results
        results.tdr?.let { tdrResults ->
            if (yPos > PAGE_BREAK_THRESHOLD - 80) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNum).create()
                )
                yPos = MARGIN
            }
            yPos = drawTdrResults(page.canvas, yPos, tdrResults)
        }

        return Pair(page, yPos)
    }

    private fun drawLinkStatus(canvas: Canvas, y: Float, linkData: Map<String, String>): Float {
        var yPos = y
        canvas.drawText("Link Status:", MARGIN + 10f, yPos, textPaint)
        yPos += LINE_HEIGHT + 2f

        linkData.forEach { (key, value) ->
            canvas.drawText("• $key: $value", MARGIN + 20f, yPos, smallTextPaint)
            yPos += LINE_HEIGHT - 2f
        }
        return yPos + 8f
    }

    private fun drawLldpNeighbors(canvas: Canvas, y: Float, neighbors: List<com.app.miklink.data.network.NeighborDetail>): Float {
        var yPos = y
        canvas.drawText("LLDP Neighbors:", MARGIN + 10f, yPos, textPaint)
        yPos += LINE_HEIGHT + 2f

        if (neighbors.isEmpty()) {
            canvas.drawText("No neighbors detected", MARGIN + 20f, yPos, smallTextPaint)
            yPos += LINE_HEIGHT
        } else {
            neighbors.forEach { neighbor ->
                canvas.drawText("• ${neighbor.identity ?: "Unknown"}", MARGIN + 20f, yPos, textPaint)
                yPos += LINE_HEIGHT

                neighbor.systemCaps?.let {
                    canvas.drawText("Capabilities: $it", MARGIN, yPos, textPaint)
                    yPos += LINE_HEIGHT
                }
            }
        }
        return yPos + 8f
    }

    private fun drawPingResults(canvas: Canvas, y: Float, pings: List<com.app.miklink.data.network.PingResult>): Float {
        var yPos = y
        canvas.drawText("Ping Test:", MARGIN + 10f, yPos, textPaint)
        yPos += LINE_HEIGHT + 2f

        pings.forEach { ping ->
            val avgRtt = ping.avgRtt ?: "N/A"
            canvas.drawText("• Average RTT: $avgRtt", MARGIN + 20f, yPos, smallTextPaint)
            yPos += LINE_HEIGHT
        }
        return yPos + 8f
    }

    private fun drawTdrResults(canvas: Canvas, y: Float, tdrResults: List<com.app.miklink.data.network.CableTestResult>): Float {
        var yPos = y
        canvas.drawText("Cable Test (TDR):", MARGIN + 10f, yPos, textPaint)
        yPos += LINE_HEIGHT + 2f

        tdrResults.forEach { tdr ->
            canvas.drawText("Status: ${tdr.status}", MARGIN + 20f, yPos, textPaint)
            yPos += LINE_HEIGHT

            tdr.cablePairs.forEachIndexed { index, pair ->
                val pairInfo = pair.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                val lines = wrapText(pairInfo, A4_WIDTH_PT - MARGIN - 50f, smallTextPaint)
                lines.forEach { line ->
                    canvas.drawText("  Pair ${index + 1}: $line", MARGIN + 30f, yPos, smallTextPaint)
                    yPos += LINE_HEIGHT - 2f
                }
            }
            yPos += 6f
        }
        return yPos + 8f
    }

    private fun drawNotesSection(canvas: Canvas, y: Float, notes: String): Float {
        var yPos = y
        canvas.drawText("Notes", MARGIN, yPos, headerPaint)
        yPos += LINE_HEIGHT + 6f
        drawSeparator(canvas, yPos)
        yPos += 8f

        // Draw notes box
        val notesLines = wrapText(notes, A4_WIDTH_PT - MARGIN * 2 - 20f, textPaint)
        val boxHeight = (notesLines.size * LINE_HEIGHT) + 16f
        val notesBox = RectF(MARGIN + 10f, yPos, A4_WIDTH_PT - MARGIN - 10f, yPos + boxHeight)
        val boxPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        canvas.drawRect(notesBox, boxPaint)

        yPos += 12f
        notesLines.forEach { line ->
            canvas.drawText(line, MARGIN + 20f, yPos, textPaint)
            yPos += LINE_HEIGHT
        }

        return yPos + 12f
    }

    private fun drawBatchIndexPage(canvas: Canvas, reports: List<Report>, client: Client?) {
        var yPos = drawHeader(canvas, MARGIN, "Project Summary: ${client?.companyName ?: "Multiple Reports"}")
        yPos += 10f

        val exportDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Export Date: $exportDate", MARGIN, yPos, textPaint)
        canvas.drawText("Total Tests: ${reports.size}", A4_WIDTH_PT - MARGIN - 120f, yPos, textPaint)
        yPos += SECTION_SPACING

        val passedCount = reports.count { it.overallStatus == "PASS" }
        val failedCount = reports.count { it.overallStatus == "FAIL" }

        canvas.drawText("Summary:", MARGIN, yPos, headerPaint)
        yPos += LINE_HEIGHT + 4f
        canvas.drawText("✓ Passed: $passedCount", MARGIN + 10f, yPos, successPaint)
        yPos += LINE_HEIGHT
        canvas.drawText("✗ Failed: $failedCount", MARGIN + 10f, yPos, failPaint)
        yPos += SECTION_SPACING

        // Table header
        canvas.drawText("Socket Name", MARGIN, yPos, headerPaint)
        canvas.drawText("Location", MARGIN + 150f, yPos, headerPaint)
        canvas.drawText("Status", MARGIN + 280f, yPos, headerPaint)
        canvas.drawText("Date", MARGIN + 360f, yPos, headerPaint)
        yPos += LINE_HEIGHT + 2f
        drawSeparator(canvas, yPos)
        yPos += 6f

        // Table rows
        reports.forEach { report ->
            if (yPos > PAGE_BREAK_THRESHOLD) return@forEach // Stop if page full

            canvas.drawText(report.socketName ?: "N/A", MARGIN, yPos, textPaint)

            val statusPaint = if (report.overallStatus == "PASS") successPaint else failPaint
            canvas.drawText(report.overallStatus, MARGIN + 280f, yPos, statusPaint)

            val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(report.timestamp))
            canvas.drawText(dateStr, MARGIN + 360f, yPos, smallTextPaint)

            yPos += LINE_HEIGHT + 2f
        }
    }

    private fun drawTestResultsSummary(canvas: Canvas, y: Float, results: ParsedResults) {
        var yPos = y
        canvas.drawText("Test Summary:", MARGIN + 10f, yPos, textPaint)
        yPos += LINE_HEIGHT + 2f

        results.link?.let {
            canvas.drawText("• Link: OK", MARGIN + 20f, yPos, smallTextPaint)
            yPos += LINE_HEIGHT
        }
        results.lldp?.let {
            canvas.drawText("• LLDP: ${it.size} neighbor(s)", MARGIN + 20f, yPos, smallTextPaint)
            yPos += LINE_HEIGHT
        }
        results.ping?.let {
            val avgRtt = it.firstOrNull()?.avgRtt ?: "N/A"
            canvas.drawText("• Ping: $avgRtt", MARGIN + 20f, yPos, smallTextPaint)
            yPos += LINE_HEIGHT
        }
        results.tdr?.let {
            canvas.drawText("• TDR: ${it.firstOrNull()?.status ?: "N/A"}", MARGIN + 20f, yPos, smallTextPaint)
        }
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
        val footerY = A4_HEIGHT_PT - MARGIN + 10f
        canvas.drawText("Generated by MikLink", MARGIN, footerY, smallTextPaint)
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText(timestamp, A4_WIDTH_PT / 2 - 40f, footerY, smallTextPaint)
        canvas.drawText("Page $pageNum of $totalPages", A4_WIDTH_PT - MARGIN - 80f, footerY, smallTextPaint)
    }

    private fun drawSeparator(canvas: Canvas, y: Float) {
        canvas.drawLine(MARGIN, y, A4_WIDTH_PT - MARGIN, y, linePaint)
    }

    /**
     * `internal` per test unitari.
     * Logica di text-wrapping manuale per il rendering su PDF.
     */
    internal fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)

            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}
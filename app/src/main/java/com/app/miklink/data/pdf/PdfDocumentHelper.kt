package com.app.miklink.data.pdf

import com.app.miklink.core.data.pdf.PdfExportConfig
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageData
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for PDF generation tasks using iText 7.
 * Encapsulates styling, header/footer creation, and common element factories.
 */
class PdfDocumentHelper {

    private val MIN_ROW_HEIGHT = 30f

    private val fontRegular by lazy { PdfFontFactory.createFont(StandardFonts.HELVETICA) }
    private val fontBold by lazy { PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD) }
    private val fontItalic by lazy { PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE) }

    private fun noBorderCell(): Cell = Cell().setBorder(Border.NO_BORDER)

    private fun paragraph(
        text: String,
        font: PdfFont,
        size: Float,
        color: Color? = null
    ): Paragraph = Paragraph(text)
        .setFont(font)
        .setFontSize(size)
        .apply { if (color != null) setFontColor(color) }

    private fun signatureCell(label: String): Cell = noBorderCell()
        .add(Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER))
        .add(Paragraph(label).setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
        .setVerticalAlignment(VerticalAlignment.BOTTOM)

    fun addHeader(document: Document, clientName: String?, title: String, logo: ImageData?) {
        val headerTable = Table(floatArrayOf(1f, 2f, 1f)) // Left (Logo), Center (Title), Right (Info)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)
            .setMarginBottom(10f)

        // Left: Logo
        val brandCell = noBorderCell()
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(TextAlignment.LEFT)
        
        if (logo != null) {
            val img = Image(logo)
            img.scaleToFit(80f, 50f)
            brandCell.add(img)
        } else {
            brandCell.add(paragraph("MIKLINK", fontBold, 18f)
                .setFontColor(DeviceRgb(46, 125, 50)))
        }

        // Center: Title
        val titleCell = noBorderCell()
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(TextAlignment.CENTER)
            .add(paragraph(title, fontBold, 16f))

        // Right: Client & Date
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = df.format(Date())
        val infoCell = noBorderCell()
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(TextAlignment.RIGHT)
            .add(paragraph(clientName ?: "Unknown Client", fontBold, 10f))
            .add(paragraph(dateStr, fontRegular, 10f, DeviceRgb(100, 100, 100)))

        headerTable.addCell(brandCell)
        headerTable.addCell(titleCell)
        headerTable.addCell(infoCell)

        document.add(headerTable)

        // Horizontal Line
        document.add(LineSeparator(SolidLine(1f))
            .setMarginBottom(20f))
    }

    fun createFooterTable(
        clientNotes: String?,
        showCpuWarning: Boolean,
        config: PdfExportConfig,
        cpuWarningText: String?
    ): Table {
        val container = Table(1)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)
        
        // CPU Warning
        // Note: replace emoji with ASCII text to avoid font issues in iText
        if (showCpuWarning) {
            val warning = paragraph(
                cpuWarningText ?: "Attenzione: carico CPU 100% (locale/remota). Valori possibilmente sottostimati.",
                fontRegular,
                10f,
                DeviceRgb(230, 81, 0)
            ) // Orange
                .setBackgroundColor(DeviceRgb(255, 248, 225))
                .setBorder(SolidBorder(DeviceRgb(255, 213, 79), 1f))
                .setPadding(6f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8f)
            container.addCell(noBorderCell().add(warning))
        }

        // Client Notes
        if (!clientNotes.isNullOrBlank()) {
            val notesCell = Cell()
                .add(paragraph("Note Cliente: $clientNotes", fontItalic, 10f, DeviceRgb(68, 68, 68)))
                .setBackgroundColor(DeviceRgb(248, 249, 250))
                .setBorder(SolidBorder(DeviceRgb(224, 224, 224), 1f))
                .setPadding(8f)
                .setMarginBottom(10f)
            container.addCell(notesCell)
        }
        
        // Signatures (if enabled)
        if (config.showSignatures) {
            val sigTable = Table(floatArrayOf(1f, 1f))
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginTop(10f)
                .setMarginBottom(10f)
                
            sigTable.addCell(signatureCell(config.signatureLeftLabel))
            sigTable.addCell(signatureCell(config.signatureRightLabel))
            container.addCell(noBorderCell().add(sigTable))
        }

        // Generation Timestamp
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        container.addCell(noBorderCell()
            .add(Paragraph("Generato il ${df.format(Date())} • MikLink")
                .setFont(fontRegular)
                .setFontSize(8f)
                .setFontColor(DeviceRgb(150, 150, 150))
                .setTextAlignment(TextAlignment.CENTER))
        )

        return container
    }

    fun createDataCell(text: String, bgColor: DeviceRgb, alignment: TextAlignment): Cell {
        return Cell().add(paragraph(text, fontRegular, 9f).setTextAlignment(alignment))
            .setBackgroundColor(bgColor)
            .setBorder(SolidBorder(DeviceRgb(221, 221, 221), 1f))
            .setPadding(4f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setMinHeight(MIN_ROW_HEIGHT)
            .setKeepTogether(true)
    }

    fun createStatusCell(status: String): Cell {
        val color = if (status.equals("PASS", true)) DeviceRgb(76, 175, 80) else DeviceRgb(244, 67, 54)
        return Cell().add(paragraph(status, fontBold, 9f, ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(color)
            .setBorder(SolidBorder(color, 1f))
            .setPadding(4f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setMinHeight(MIN_ROW_HEIGHT)
            .setKeepTogether(true)
    }

    /**
     * Event handler for adding page numbers to each page
     */
    class PageNumberEventHandler : IEventHandler {
        override fun handleEvent(event: Event) {
            val docEvent = event as PdfDocumentEvent
            val pdfDoc = docEvent.document
            val page = docEvent.page
            val pageNumber = pdfDoc.getPageNumber(page)
            val pageSize = page.pageSize

            val canvas = PdfCanvas(page)
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
}

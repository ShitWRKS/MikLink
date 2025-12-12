package com.app.miklink.core.data.pdf

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
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

    fun addHeader(document: Document, clientName: String?, title: String, logo: com.itextpdf.io.image.ImageData?) {
        val headerTable = Table(floatArrayOf(1f, 2f, 1f)) // Left (Logo), Center (Title), Right (Info)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setMarginBottom(10f)

        // Left: Logo
        val brandCell = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(TextAlignment.LEFT)
        
        if (logo != null) {
            val img = com.itextpdf.layout.element.Image(logo)
            img.scaleToFit(80f, 50f)
            brandCell.add(img)
        } else {
            brandCell.add(Paragraph("MIKLINK")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(18f)
                .setFontColor(DeviceRgb(46, 125, 50)))
        }

        // Center: Title
        val titleCell = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(TextAlignment.CENTER)
            .add(Paragraph(title)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(16f))

        // Right: Client & Date
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = df.format(Date())
        val infoCell = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(TextAlignment.RIGHT)
            .add(Paragraph(clientName ?: "Unknown Client")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(10f))
            .add(Paragraph(dateStr)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(10f)
                .setFontColor(DeviceRgb(100, 100, 100)))

        headerTable.addCell(brandCell)
        headerTable.addCell(titleCell)
        headerTable.addCell(infoCell)

        document.add(headerTable)

        // Horizontal Line
        document.add(com.itextpdf.layout.element.LineSeparator(com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
            .setMarginBottom(20f))
    }

    fun createFooterTable(
        clientNotes: String?, 
        showCpuWarning: Boolean, 
        config: PdfExportConfig
    ): Table {
        val container = Table(1)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
        
        // CPU Warning
        // Note: replace emoji with ASCII text to avoid font issues in iText
        if (showCpuWarning) {
            val warning = Paragraph("Attenzione: carico CPU 100% (locale/remota). Valori possibilmente sottostimati.")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(10f)
                .setFontColor(DeviceRgb(230, 81, 0)) // Orange
                .setBackgroundColor(DeviceRgb(255, 248, 225))
                .setBorder(SolidBorder(DeviceRgb(255, 213, 79), 1f))
                .setPadding(6f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8f)
            container.addCell(Cell().add(warning).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        }

        // Client Notes
        if (!clientNotes.isNullOrBlank()) {
            val notesCell = Cell()
                .add(Paragraph("Note Cliente: $clientNotes")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE))
                    .setFontSize(10f)
                    .setFontColor(DeviceRgb(68, 68, 68)))
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
                
            val leftSig = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .add(Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER))
                .add(Paragraph(config.signatureLeftLabel).setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
                .setVerticalAlignment(VerticalAlignment.BOTTOM)
                
            val rightSig = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .add(Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER))
                .add(Paragraph(config.signatureRightLabel).setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
                .setVerticalAlignment(VerticalAlignment.BOTTOM)
                
            sigTable.addCell(leftSig)
            sigTable.addCell(rightSig)
            container.addCell(Cell().add(sigTable).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        }

        // Generation Timestamp
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        container.addCell(Cell()
            .add(Paragraph("Generato il ${df.format(Date())} • MikLink")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(8f)
                .setFontColor(DeviceRgb(150, 150, 150))
                .setTextAlignment(TextAlignment.CENTER))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
        )

        return container
    }

    fun createDataCell(text: String, bgColor: DeviceRgb, alignment: TextAlignment): Cell {
        return Cell().add(Paragraph(text).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA)).setFontSize(9f).setTextAlignment(alignment))
            .setBackgroundColor(bgColor)
            .setBorder(SolidBorder(DeviceRgb(221, 221, 221), 1f))
            .setPadding(4f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }

    fun createStatusCell(status: String): Cell {
        val color = if (status.equals("PASS", true)) DeviceRgb(76, 175, 80) else DeviceRgb(244, 67, 54)
        return Cell().add(Paragraph(status).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(9f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(color)
            .setBorder(SolidBorder(color, 1f))
            .setPadding(4f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }

    /**
     * Event handler for adding page numbers to each page
     */
    class PageNumberEventHandler : com.itextpdf.kernel.events.IEventHandler {
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
}


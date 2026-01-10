package com.app.miklink.core.data.pdf

enum class ExportColumn {
    SOCKET,
    DATE,
    STATUS,
    LINK_SPEED,
    NEIGHBOR,
    PING,
    TDR,
    SPEED_TEST
}

enum class PdfPageOrientation {
    PORTRAIT,
    LANDSCAPE
}

data class PdfExportConfig(
    val title: String,
    val includeEmptyTests: Boolean,
    val columns: List<ExportColumn>,
    val showSignatures: Boolean,
    val signatureLeftLabel: String,
    val signatureRightLabel: String,
    val orientation: PdfPageOrientation = PdfPageOrientation.LANDSCAPE,
    val hideEmptyColumns: Boolean = false
)


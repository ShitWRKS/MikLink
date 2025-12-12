package com.app.miklink.core.data.pdf

enum class ExportColumn(val label: String) {
    SOCKET("Presa"),
    DATE("Data/Ora"),
    STATUS("Stato"),
    LINK_SPEED("Link Speed"),
    NEIGHBOR("Neighbor"),
    PING("Ping"),
    TDR("TDR"),
    SPEED_TEST("Speed Test")
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


package com.app.miklink.core.domain.model

data class TestReport(
    val reportId: Long = 0L,
    val clientId: Long?,
    val timestamp: Long,
    val socketName: String?,
    val notes: String?,
    val probeName: String?,
    val profileName: String?,
    val overallStatus: String,
    val resultFormatVersion: Int = 1,
    val resultsJson: String
)

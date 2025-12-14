package com.app.miklink.ui.history.model

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.TestReport

data class ReportsByClient(
    val client: Client?,
    val reports: List<TestReport>,
    val totalTests: Int = reports.size,
    val passedTests: Int = reports.count { it.overallStatus == "PASS" },
    val failedTests: Int = reports.count { it.overallStatus == "FAIL" },
    val lastTestDate: Long = reports.maxOfOrNull { it.timestamp } ?: 0L
)


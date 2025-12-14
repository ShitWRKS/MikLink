package com.app.miklink.core.domain.test.model

import com.app.miklink.core.domain.model.report.TdrEntry

/**
 * Domain representation of a TDR/cable test execution.
 */
data class CableTestSummary(
    val status: String?,
    val entries: List<TdrEntry>
)

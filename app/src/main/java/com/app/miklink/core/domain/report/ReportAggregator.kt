package com.app.miklink.core.domain.report

/**
 * ReportAggregator - placeholder for report aggregation rules
 *
 * Responsibilities:
 * - Interpret `resultsJson` (single JSON field) into domain structures, derive overall report status
 * - Provide helper functions to extract metrics for reporting
 *
 * Design decision:
 * - The current schema keeps `resultsJson` as a single JSON column. Transformations into
 *   structured columns or analytics-ready formats are deferred to a future epic named
 *   `Report Analytics`.
 */
interface ReportAggregator {
    // fun aggregate(resultsJson: String): AggregatedReport
}

data class AggregatedReport(val overallStatus: String)

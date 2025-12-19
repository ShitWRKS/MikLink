/*
 * Purpose: Immutable summary of a completed test run for UI completion and report saving.
 * Inputs: Overall status string, final typed snapshot, optional raw results JSON for persistence.
 * Outputs: Data consumed by UI to build TestReport and by storage to persist resultsJson.
 * Notes: Legacy section list removed per ADR-0011; typed snapshot is the single contract.
 */
package com.app.miklink.core.domain.test.model

/**
 * Output consumato da UI + persistenza Report.
 * Contiene lo stato complessivo del test e il payload finale tipizzato.
 */
data class TestOutcome(
    val overallStatus: String, // PASS, FAIL
    val finalSnapshot: TestRunSnapshot,
    val rawResultsJson: String? = null // JSON serializzato per compatibilità con Report esistente
)

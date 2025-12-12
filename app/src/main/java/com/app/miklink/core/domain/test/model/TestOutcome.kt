package com.app.miklink.core.domain.test.model

/**
 * Output consumato da UI + persistenza Report.
 * Contiene lo stato complessivo del test e i risultati delle singole sezioni.
 */
data class TestOutcome(
    val overallStatus: String, // PASS, FAIL
    val sections: List<TestSectionResult> = emptyList(),
    val rawResultsJson: String? = null // JSON serializzato per compatibilità con Report esistente
)


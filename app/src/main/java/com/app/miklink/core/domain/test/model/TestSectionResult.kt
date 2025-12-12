package com.app.miklink.core.domain.test.model

/**
 * Risultato di una sezione di test (Link, TDR, Ping, ecc.)
 */
data class TestSectionResult(
    val type: String,
    val title: String,
    val status: String, // PASS, FAIL, WARN, SKIP, INFO
    val details: Map<String, String> = emptyMap()
)


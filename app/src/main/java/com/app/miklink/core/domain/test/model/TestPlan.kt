package com.app.miklink.core.domain.test.model

/**
 * Input per avvio test; non contiene stato runtime.
 * Definisce i parametri necessari per eseguire un test.
 */
data class TestPlan(
    val clientId: Long,
    val probeId: Long,
    val profileId: Long,
    val socketId: String? = null,
    val notes: String? = null
)


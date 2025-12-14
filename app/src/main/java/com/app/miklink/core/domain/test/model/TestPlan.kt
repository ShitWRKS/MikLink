package com.app.miklink.core.domain.test.model

/**
 * Input per avvio test; non contiene stato runtime.
 * Definisce i parametri necessari per eseguire un test.
 * 
 * La sonda è SINGLETON: non si specifica un identificatore qui.
 * RunTestUseCase carica automaticamente la ProbeConfig unica.
 */
data class TestPlan(
    val clientId: Long,
    val profileId: Long,
    val socketId: String? = null,
    val notes: String? = null
)


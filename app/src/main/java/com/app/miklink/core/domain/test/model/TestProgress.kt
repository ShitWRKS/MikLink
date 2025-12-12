package com.app.miklink.core.domain.test.model

/**
 * Stato progressivo del test: step corrente, percentuale, messaggio UI.
 * Non legato a Compose, può essere usato da qualsiasi layer di presentazione.
 */
data class TestProgress(
    val currentStep: String,
    val percentage: Int,
    val message: String
)


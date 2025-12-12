package com.app.miklink.core.domain.test.model

/**
 * Risultato di un singolo step di test.
 */
sealed class StepResult {
    data class Success(val data: Any) : StepResult()
    data class Skipped(val reason: String) : StepResult()
    data class Failed(val error: TestError) : StepResult()
}


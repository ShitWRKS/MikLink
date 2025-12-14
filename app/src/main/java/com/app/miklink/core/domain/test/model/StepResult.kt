package com.app.miklink.core.domain.test.model

/**
 * Risultato di un singolo step di test.
 */
sealed class StepResult<out T> {
    data class Success<T>(val data: T) : StepResult<T>()
    data class Skipped(val reason: String) : StepResult<Nothing>()
    data class Failed(val error: TestError) : StepResult<Nothing>()
}


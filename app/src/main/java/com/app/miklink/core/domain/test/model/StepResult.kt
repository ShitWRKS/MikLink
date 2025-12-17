/*
 * Purpose: Represent outcomes of individual test steps in a type-safe way.
 * Inputs: Optional success payload, skip reason, or typed TestError with optional partial data.
 * Outputs: Sealed variants consumed by use cases to drive flow control and UI/reporting.
 * Notes: Failed variant can carry partial data (e.g., ping outcomes) to surface details even on errors.
 */
package com.app.miklink.core.domain.test.model

/**
 * Risultato di un singolo step di test.
 */
sealed class StepResult<out T> {
    data class Success<T>(val data: T) : StepResult<T>()
    data class Skipped(val reason: String) : StepResult<Nothing>()
    data class Failed<T>(val error: TestError, val data: T? = null) : StepResult<T>()
}

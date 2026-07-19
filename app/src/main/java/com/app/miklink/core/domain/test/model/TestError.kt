/*
 * Purpose: Represent domain-level errors that can occur during test execution.
 * Inputs: Error messages and optional causes from test steps.
 * Outputs: Typed errors used for flow control and user feedback.
 * Notes: Extend with additional types if new failure modes emerge.
 */
package com.app.miklink.core.domain.test.model

/**
 * Errori possibili durante l'esecuzione di un test.
 * Sealed class per type-safe error handling.
 */
sealed class TestError {
    abstract val message: String

    data class ProbeUnavailable(override val message: String, val cause: Throwable? = null) : TestError()
    data class Authentication(override val message: String, val cause: Throwable? = null) : TestError()
    data class Tls(override val message: String, val cause: Throwable? = null) : TestError()
    data class Timeout(override val message: String, val cause: Throwable? = null) : TestError()
    data class RouterOsError(
        override val message: String,
        val code: Int? = null,
        val detail: String? = null
    ) : TestError()
    data class InvalidResponse(override val message: String, val cause: Throwable? = null) : TestError()
    data class Unsupported(override val message: String) : TestError()
    data class ConfigurationError(override val message: String) : TestError()
    data class SerializationError(override val message: String, val cause: Throwable? = null) : TestError()
    data class Unexpected(override val message: String, val cause: Throwable? = null) : TestError()
}

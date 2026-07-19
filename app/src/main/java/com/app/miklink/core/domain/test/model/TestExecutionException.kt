/*
 * Purpose: Single application exception that transports a typed TestError through APIs that expose exceptions.
 * Inputs: Any TestError variant produced by RouterOsResponseDecoder or domain logic.
 * Outputs: Exception suitable for propagation across repository/step/runner boundaries.
 * Notes: This is the ONLY wrapper used to transport a typed error. Do not convert TestError
 *        into SecurityException, IOException, IllegalStateException, or similar generic exceptions.
 */
package com.app.miklink.core.domain.test.model

/**
 * Application exception carrying a typed [TestError] without altering it.
 *
 * Replaces the previous pattern of converting TestError into generic Java exceptions
 * (e.g. Authentication → SecurityException, ProbeUnavailable → IOException) which
 * caused loss of type information during re-classification.
 */
class TestExecutionException(
    val error: TestError
) : Exception(
    error.message,
    error.extractCause()
)

/**
 * Extracts the cause Throwable from a TestError, if available.
 */
private fun TestError.extractCause(): Throwable? = when (this) {
    is TestError.ProbeUnavailable -> cause
    is TestError.Authentication -> cause
    is TestError.Tls -> cause
    is TestError.Timeout -> cause
    is TestError.InvalidResponse -> cause
    is TestError.SerializationError -> cause
    is TestError.Unexpected -> cause
    is TestError.RouterOsError -> null
    is TestError.Unsupported -> null
    is TestError.ConfigurationError -> null
}

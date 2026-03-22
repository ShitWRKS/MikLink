/*
 * Purpose: Execute MikroTik API calls with HTTPS->HTTP fallback while preserving transport metadata.
 * Inputs: Probe configuration and MikroTikServiceProvider to build bound MikroTikApiService instances.
 * Outputs: CallOutcome results with TransportMeta (or legacy MikroTikCallResult) plus aggregated failures.
 * Notes: Trust-all and socket binding stay in MikroTikServiceFactory/MikroTikServiceProviderImpl (ADR-0002).
 */
package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.core.domain.model.ProbeConfig
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException

data class TransportMeta(
    val attemptedHttps: Boolean,
    val effectiveIsHttps: Boolean,
    val didFallbackToHttp: Boolean
)

data class AttemptFailure(
    val scheme: String,
    val throwable: Throwable
)

sealed interface CallOutcome<out T> {
    data class Success<T>(val value: T, val meta: TransportMeta) : CallOutcome<T>
    data class Failure(val meta: TransportMeta, val failures: List<AttemptFailure>) : CallOutcome<Nothing>
}

@Deprecated("Use CallOutcome via executeWithOutcome to access transport metadata and errors")
data class MikroTikCallResult<T>(
    val value: T,
    val didFallbackToHttp: Boolean,
    val effectiveIsHttps: Boolean
)

/**
 * Centralizza la policy di invocazione MikroTik con fallback HTTPS->HTTP.
 */
class MikroTikCallExecutor @Inject constructor(
    private val serviceProvider: MikroTikServiceProvider
) {
    private val logTag = "MikroTikCallExecutor"

    suspend fun <T> executeWithOutcome(
        probe: ProbeConfig,
        block: suspend (MikroTikApiService) -> T
    ): CallOutcome<T> {
        if (probe.isHttps) {
            val httpsApi = serviceProvider.build(probe)
            try {
                val value = block(httpsApi)
                return CallOutcome.Success(
                    value = value,
                    meta = TransportMeta(
                        attemptedHttps = true,
                        effectiveIsHttps = true,
                        didFallbackToHttp = false
                    )
                )
            } catch (handshake: SSLHandshakeException) {
                logWarn("HTTPS handshake failed for ${probe.ipAddress}, retrying over HTTP.", handshake)
                val fallbackProbe = probe.copy(isHttps = false)
                val httpApi = serviceProvider.build(fallbackProbe)
                return try {
                    val value = block(httpApi)
                    CallOutcome.Success(
                        value = value,
                        meta = TransportMeta(
                            attemptedHttps = true,
                            effectiveIsHttps = false,
                            didFallbackToHttp = true
                        )
                    )
                } catch (httpError: Exception) {
                    if (httpError is CancellationException) throw httpError
                    logWarn(
                        "HTTP fallback after TLS handshake failure also failed for ${probe.ipAddress}.",
                        httpError
                    )
                    CallOutcome.Failure(
                        meta = TransportMeta(
                            attemptedHttps = true,
                            effectiveIsHttps = false,
                            didFallbackToHttp = true
                        ),
                        failures = listOf(
                            AttemptFailure(scheme = "https", throwable = handshake),
                            AttemptFailure(scheme = "http", throwable = httpError)
                        )
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                logError("HTTPS call failed for ${probe.ipAddress}", error)
                return CallOutcome.Failure(
                    meta = TransportMeta(
                        attemptedHttps = true,
                        effectiveIsHttps = true,
                        didFallbackToHttp = false
                    ),
                    failures = listOf(AttemptFailure(scheme = "https", throwable = error))
                )
            }
        }

        val api = serviceProvider.build(probe)
        return try {
            val value = block(api)
            CallOutcome.Success(
                value = value,
                meta = TransportMeta(
                    attemptedHttps = false,
                    effectiveIsHttps = false,
                    didFallbackToHttp = false
                )
            )
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logError("HTTP call failed for ${probe.ipAddress}", error)
            CallOutcome.Failure(
                meta = TransportMeta(
                    attemptedHttps = false,
                    effectiveIsHttps = false,
                    didFallbackToHttp = false
                ),
                failures = listOf(AttemptFailure(scheme = "http", throwable = error))
            )
        }
    }

    @Deprecated("Use executeWithOutcome to inspect CallOutcome and transport metadata")
    @Suppress("DEPRECATION")
    suspend fun <T> execute(probe: ProbeConfig, block: suspend (MikroTikApiService) -> T): MikroTikCallResult<T> {
        return when (val outcome = executeWithOutcome(probe, block)) {
            is CallOutcome.Success -> MikroTikCallResult(
                value = outcome.value,
                didFallbackToHttp = outcome.meta.didFallbackToHttp,
                effectiveIsHttps = outcome.meta.effectiveIsHttps
            )
            is CallOutcome.Failure -> throw aggregateFailures(outcome.failures)
        }
    }

    private fun logWarn(message: String, error: Throwable) {
        if (android.util.Log.isLoggable(logTag, android.util.Log.WARN)) {
            android.util.Log.w(logTag, message, error)
        }
    }

    private fun logError(message: String, error: Throwable) {
        if (android.util.Log.isLoggable(logTag, android.util.Log.ERROR)) {
            android.util.Log.e(logTag, message, error)
        }
    }

    private fun aggregateFailures(failures: List<AttemptFailure>): Throwable {
        val primary = failures.firstOrNull()?.throwable
            ?: IllegalStateException("CallOutcome.Failure without root cause")
        failures.drop(1).forEach { primary.addSuppressed(it.throwable) }
        return primary
    }
}

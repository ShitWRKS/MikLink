package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.core.domain.model.ProbeConfig
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

data class MikroTikCallResult<T>(
    val value: T,
    val didFallbackToHttp: Boolean,
    val effectiveIsHttps: Boolean
)

/**
 * Centralizza la policy di invocazione MikroTik con fallback HTTPS→HTTP.
 */
class MikroTikCallExecutor @Inject constructor(
    private val serviceProvider: MikroTikServiceProvider
) {
    private val logTag = "MikroTikCallExecutor"

    suspend fun <T> execute(probe: ProbeConfig, block: suspend (MikroTikApiService) -> T): MikroTikCallResult<T> {
        return try {
            val api = serviceProvider.build(probe)
            MikroTikCallResult(
                value = block(api),
                didFallbackToHttp = false,
                effectiveIsHttps = probe.isHttps
            )
        } catch (error: SSLHandshakeException) {
            if (!probe.isHttps) throw error
            if (android.util.Log.isLoggable(logTag, android.util.Log.WARN)) {
                android.util.Log.w(
                    logTag,
                    "HTTPS handshake failed for ${probe.ipAddress}, retrying over HTTP.",
                    error
                )
            }
            val fallbackProbe = probe.copy(isHttps = false)
            val fallbackApi = serviceProvider.build(fallbackProbe)
            try {
                MikroTikCallResult(
                    value = block(fallbackApi),
                    didFallbackToHttp = true,
                    effectiveIsHttps = false
                )
            } catch (fallbackError: Exception) {
                if (android.util.Log.isLoggable(logTag, android.util.Log.WARN)) {
                    android.util.Log.w(
                        logTag,
                        "HTTP fallback after TLS handshake failure also failed.",
                        fallbackError
                    )
                }
                fallbackError.addSuppressed(error)
                throw error
            }
        }
    }
}

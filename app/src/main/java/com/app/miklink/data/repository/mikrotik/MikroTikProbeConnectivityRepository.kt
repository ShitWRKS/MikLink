/*
 * Purpose: Check MikroTik probe connectivity and surface board/interface metadata to the app layer.
 * Inputs: Probe configuration, MikroTikCallExecutor, and application context for localized errors.
 * Outputs: ProbeCheckResult indicating success with metadata or an error message.
 * Notes: Keeps single transport path via MikroTikCallExecutor (ADR-0007/ADR-0002).
 */
package com.app.miklink.data.repository.mikrotik

import android.content.Context
import com.app.miklink.R
import com.app.miklink.core.data.repository.ProbeCheckResult
import com.app.miklink.core.data.repository.probe.ProbeConnectivityRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.dto.ProplistRequest
import com.app.miklink.data.remote.mikrotik.service.AttemptFailure
import com.app.miklink.data.remote.mikrotik.service.CallOutcome
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

/**
 * Implementazione MikroTik di ProbeConnectivityRepository.
 *
 * Usa MikroTikCallExecutor per applicare la policy HTTPS->HTTP e per recuperare board-name e interfacce.
 */
class MikroTikProbeConnectivityRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val callExecutor: MikroTikCallExecutor
) : ProbeConnectivityRepository {

    private val logTag = "ProbeConnectivityRepository"

    override suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult =
        withContext(Dispatchers.IO) {
            val outcome = callExecutor.executeWithOutcome(probe) { api ->
                fetchProbeMetadata(api)
            }
            when (outcome) {
                is CallOutcome.Success -> {
                    val (boardName, interfaces) = outcome.value
                    ProbeCheckResult.Success(
                        boardName = boardName,
                        interfaces = interfaces,
                        effectiveIsHttps = outcome.meta.effectiveIsHttps,
                        didFallbackToHttp = outcome.meta.didFallbackToHttp,
                        warning = if (outcome.meta.didFallbackToHttp) {
                            context.getString(R.string.probe_verify_http_fallback_warning)
                        } else null
                    )
                }
                is CallOutcome.Failure -> {
                    logFailures(outcome.failures)
                    val message = ProbeErrorMapper.toMessage(
                        error = outcome.failures.firstOrNull()?.throwable
                            ?: IllegalStateException("Unknown call failure"),
                        defaultMessage = context.getString(R.string.error_probe_connection_unknown),
                        handshakeMessage = context.getString(R.string.error_probe_connection_tls_handshake)
                    )
                    ProbeCheckResult.Error(message)
                }
            }
        }

    private suspend fun fetchProbeMetadata(api: MikroTikApiService): Pair<String, List<String>> {
        val systemResources = api.getSystemResource(ProplistRequest(listOf("board-name")))
        if (android.util.Log.isLoggable(logTag, android.util.Log.DEBUG)) {
            android.util.Log.d(
                logTag,
                "systemResource response: ${systemResources.joinToString { it.boardName ?: "<null>" }}"
            )
        }
        val boardName = systemResources
            .mapNotNull { it.boardName?.trim()?.takeIf { name -> name.isNotEmpty() } }
            .firstOrNull()
            ?: "Unknown Board"
        val interfacesRaw = api.getEthernetInterfaces()
        if (android.util.Log.isLoggable(logTag, android.util.Log.DEBUG)) {
            android.util.Log.d(logTag, "checkProbeConnection: Ricevute ${interfacesRaw.size} interfacce dall'API")
        }
        val interfaces = interfacesRaw.map {
            if (android.util.Log.isLoggable(logTag, android.util.Log.DEBUG)) {
                android.util.Log.d(logTag, "checkProbeConnection: Interface name = '${it.name}'")
            }
            it.name
        }
        if (android.util.Log.isLoggable(logTag, android.util.Log.DEBUG)) {
            android.util.Log.d(logTag, "checkProbeConnection: Interfacce mappate: $interfaces")
        }
        return boardName to interfaces
    }

    private fun logFailures(failures: List<AttemptFailure>) {
        failures.forEach { attempt ->
            if (attempt.throwable is SSLHandshakeException &&
                android.util.Log.isLoggable(logTag, android.util.Log.WARN)
            ) {
                android.util.Log.w(
                    logTag,
                    "TLS handshake failed during probe verification over ${attempt.scheme}.",
                    attempt.throwable
                )
            } else if (android.util.Log.isLoggable(logTag, android.util.Log.ERROR)) {
                android.util.Log.e(
                    logTag,
                    "Probe verification failed over ${attempt.scheme}.",
                    attempt.throwable
                )
            }
        }
    }
}

/*
 * Purpose: Resolve MikroTik DHCP gateway information through the centralized transport executor.
 * Inputs: Probe configuration, target interface name, and MikroTikCallExecutor.
 * Outputs: Gateway IP (nullable) obtained via DHCP client status.
 * Notes: Keeps HTTPS->HTTP fallback centralized (ADR-0002) and avoids leaking transport details to callers.
 */
package com.app.miklink.data.repository.mikrotik

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.data.remote.mikrotik.service.CallOutcome
import com.app.miklink.data.remote.mikrotik.service.DecodedResult
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.data.remote.mikrotik.service.RouterOsOperation
import com.app.miklink.data.remote.mikrotik.service.RouterOsResponseDecoder
import com.app.miklink.core.data.repository.test.DhcpGatewayRepository
import javax.inject.Inject

/**
 * Implementazione MikroTik di DhcpGatewayRepository.
 *
 * Usa MikroTikCallExecutor per applicare il fallback HTTPS->HTTP prima di interrogare il client DHCP.
 */
class MikroTikDhcpGatewayRepository @Inject constructor(
    private val callExecutor: MikroTikCallExecutor,
    private val decoder: RouterOsResponseDecoder
) : DhcpGatewayRepository {

    override suspend fun getGatewayForInterface(
        probe: ProbeConfig,
        interfaceName: String
    ): String? {
        return try {
            val outcome = callExecutor.executeWithOutcome(probe) { api ->
                val response = api.getDhcpClientStatus(interfaceName)
                when (val decoded = decoder.decode(RouterOsOperation.DHCP_CLIENT_STATUS, response)) {
                    is DecodedResult.Error -> throw mapToTransportException(decoded.error)
                    is DecodedResult.Success -> decoded.value.firstOrNull()?.gateway
                }
            }
            outcome.getOrThrow(callExecutor)
        } catch (_: Exception) {
            // In caso di errore rete/API, ritorna null invece di propagare l'eccezione
            null
        }
    }
}

private fun <T> CallOutcome<T>.getOrThrow(executor: MikroTikCallExecutor): T {
    return when (this) {
        is CallOutcome.Success -> value
        is CallOutcome.Failure -> {
            val primary = failures.firstOrNull()?.throwable ?: IllegalStateException("Unknown call failure")
            failures.drop(1).forEach { primary.addSuppressed(it.throwable) }
            throw mapToTransportException(executor.classify(primary))
        }
    }
}

private fun mapToTransportException(error: TestError): Exception {
    return when (error) {
        is TestError.ProbeUnavailable -> java.io.IOException(error.message, error.cause)
        is TestError.Authentication -> SecurityException(error.message)
        is TestError.Tls -> javax.net.ssl.SSLHandshakeException(error.message)
        is TestError.Timeout -> java.net.SocketTimeoutException(error.message)
        is TestError.RouterOsError -> com.app.miklink.data.remote.mikrotik.service.RouterOsTransportException(error)
        is TestError.InvalidResponse -> IllegalStateException(error.message)
        is TestError.Unsupported -> UnsupportedOperationException(error.message)
        is TestError.ConfigurationError -> IllegalStateException(error.message)
        is TestError.SerializationError -> IllegalStateException(error.message, error.cause)
        is TestError.Unexpected -> IllegalStateException(error.message, error.cause)
    }
}

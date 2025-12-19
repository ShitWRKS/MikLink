/*
 * Purpose: Resolve MikroTik DHCP gateway information through the centralized transport executor.
 * Inputs: Probe configuration, target interface name, and MikroTikCallExecutor.
 * Outputs: Gateway IP (nullable) obtained via DHCP client status.
 * Notes: Keeps HTTPS->HTTP fallback centralized (ADR-0002) and avoids leaking transport details to callers.
 */
package com.app.miklink.data.repository.mikrotik

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.service.CallOutcome
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.core.data.repository.test.DhcpGatewayRepository
import javax.inject.Inject

/**
 * Implementazione MikroTik di DhcpGatewayRepository.
 *
 * Usa MikroTikCallExecutor per applicare il fallback HTTPS->HTTP prima di interrogare il client DHCP.
 */
class MikroTikDhcpGatewayRepository @Inject constructor(
    private val callExecutor: MikroTikCallExecutor
) : DhcpGatewayRepository {

    override suspend fun getGatewayForInterface(
        probe: ProbeConfig,
        interfaceName: String
    ): String? {
        return try {
            val outcome = callExecutor.executeWithOutcome(probe) { api ->
                api.getDhcpClientStatus(interfaceName).firstOrNull()?.gateway
            }
            outcome.getOrThrow()
        } catch (_: Exception) {
            // In caso di errore rete/API, ritorna null invece di propagare l'eccezione
            null
        }
    }
}

private fun <T> CallOutcome<T>.getOrThrow(): T {
    return when (this) {
        is CallOutcome.Success -> value
        is CallOutcome.Failure -> {
            val primary = failures.firstOrNull()?.throwable ?: IllegalStateException("Unknown call failure")
            failures.drop(1).forEach { primary.addSuppressed(it.throwable) }
            throw primary
        }
    }
}

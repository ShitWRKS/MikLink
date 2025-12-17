package com.app.miklink.data.repositoryimpl.mikrotik

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.core.data.repository.test.DhcpGatewayRepository
import javax.inject.Inject

/**
 * Implementazione di DhcpGatewayRepository.
 * 
 * Usa MikroTikCallExecutor per creare il service e applicare la policy HTTPS→HTTP fallback
 * prima di chiamare getDhcpClientStatus per ottenere il gateway DHCP.
 */
class DhcpGatewayRepositoryImpl @Inject constructor(
    private val callExecutor: MikroTikCallExecutor
) : DhcpGatewayRepository {

    override suspend fun getGatewayForInterface(
        probe: ProbeConfig,
        interfaceName: String
    ): String? {
        return try {
            callExecutor.execute(probe) { api ->
                api.getDhcpClientStatus(interfaceName).firstOrNull()?.gateway
            }.value
        } catch (_: Exception) {
            // In caso di errore rete/API, ritorna null invece di propagare l'eccezione
            // Questo permette al chiamante di gestire il caso "gateway non disponibile"
            null
        }
    }
}

package com.app.miklink.data.repositoryimpl.mikrotik

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.core.data.repository.test.DhcpGatewayRepository
import javax.inject.Inject

/**
 * Implementazione di DhcpGatewayRepository.
 * 
 * Usa MikroTikServiceProvider per costruire il service e chiama getDhcpClientStatus
 * per ottenere il gateway DHCP.
 */
class DhcpGatewayRepositoryImpl @Inject constructor(
    private val serviceProvider: MikroTikServiceProvider
) : DhcpGatewayRepository {

    override suspend fun getGatewayForInterface(
        probe: ProbeConfig,
        interfaceName: String
    ): String? {
        return try {
            val api = serviceProvider.build(probe)
            api.getDhcpClientStatus(interfaceName).firstOrNull()?.gateway
        } catch (_: Exception) {
            // In caso di errore rete/API, ritorna null invece di propagare l'eccezione
            // Questo permette al chiamante di gestire il caso "gateway non disponibile"
            null
        }
    }
}


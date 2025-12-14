package com.app.miklink.core.data.repository.test

import com.app.miklink.core.domain.model.ProbeConfig

/**
 * Repository per ottenere il gateway DHCP da un'interfaccia MikroTik.
 * 
 * Estrae la logica di chiamata API DHCP dal PingTargetResolver per rispettare SRP.
 */
interface DhcpGatewayRepository {
    /**
     * Ottiene il gateway DHCP per l'interfaccia specificata.
     * 
     * @param probe Configurazione della sonda MikroTik
     * @param interfaceName Nome dell'interfaccia di rete
     * @return Indirizzo IP del gateway DHCP, oppure null se non disponibile o in caso di errore
     */
    suspend fun getGatewayForInterface(
        probe: ProbeConfig,
        interfaceName: String
    ): String?
}


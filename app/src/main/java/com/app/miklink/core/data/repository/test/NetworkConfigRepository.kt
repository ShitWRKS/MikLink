package com.app.miklink.core.data.repository.test

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.data.repository.NetworkConfigFeedback

/**
 * Repository per applicare la configurazione di rete di un client su una sonda MikroTik.
 *
 * Gestisce sia configurazione DHCP che STATIC, replicando la logica di configurazione
 * senza dipendere da AppRepository (EPIC S6).
 */
interface NetworkConfigRepository {
    /**
     * Applica la configurazione di rete del client sulla sonda MikroTik.
     *
     * @param probe Configurazione della sonda MikroTik
     * @param client Client con configurazione di rete da applicare
     * @param override Client opzionale per override temporaneo (per singolo test)
     * @return Feedback sulla configurazione applicata
     * @throws IllegalStateException se la configurazione fallisce o se mancano dati obbligatori
     */
    suspend fun applyClientNetworkConfig(
        probe: ProbeConfig,
        client: Client,
        override: Client? = null
    ): NetworkConfigFeedback
}


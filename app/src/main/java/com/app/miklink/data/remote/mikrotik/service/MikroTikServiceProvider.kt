package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.core.domain.model.ProbeConfig

/**
 * Provider per creare istanze di MikroTikApiService per un dato ProbeConfig.
 * 
 * Centralizza la logica di costruzione del service, inclusa la gestione del WiFi network binding
 * quando necessario.
 */
interface MikroTikServiceProvider {
    /**
     * Crea un'istanza di MikroTikApiService configurata per il probe specificato.
     * 
     * @param probe Configurazione della sonda MikroTik
     * @return Service API configurato e pronto all'uso
     */
    fun build(probe: ProbeConfig): MikroTikApiService
}


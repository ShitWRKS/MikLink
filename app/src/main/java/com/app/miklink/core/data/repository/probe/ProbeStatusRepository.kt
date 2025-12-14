package com.app.miklink.core.data.repository.probe

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.data.repository.ProbeStatusInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository per monitoraggio dello stato online/offline delle sonde MikroTik.
 *
 * Gestisce il polling periodico dello stato delle sonde tramite chiamate API MikroTik.
 * Il polling interval è configurato tramite UserPreferencesRepository (non dipendenza diretta).
 *
 * Input: ProbeConfig (singola sonda o lista)
 * Output: Flow<Boolean> (singola sonda) o Flow<List<ProbeStatusInfo>> (tutte le sonde)
 * Error policy: Gli errori di rete vengono gestiti internamente ritornando false (offline)
 *               per evitare di interrompere il Flow. Nessuna eccezione propagata.
 */
interface ProbeStatusRepository {
    /**
     * Osserva lo stato online/offline di una singola sonda.
     *
     * Esegue polling periodico chiamando l'API MikroTik per verificare se la sonda è raggiungibile.
     * Il polling interval è determinato dalle preferenze utente.
     *
     * @param probe Configurazione della sonda da monitorare
     * @return Flow che emette periodicamente true se la sonda è online, false altrimenti
     */
    fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>

    /**
     * Osserva lo stato online/offline di tutte le sonde configurate.
     *
     * Combina i dati dal database (lista sonde) con polling periodico per verificare lo stato.
     * Il polling interval è determinato dalle preferenze utente.
     *
     * @return Flow che emette periodicamente una lista di ProbeStatusInfo con lo stato aggiornato
     *         di tutte le sonde. Se non ci sono sonde configurate, emette lista vuota.
     */
    fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>
}


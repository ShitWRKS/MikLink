package com.app.miklink.core.data.repository.test

import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.remote.mikrotik.dto.CableTestResult
import com.app.miklink.core.data.remote.mikrotik.dto.MonitorResponse
import com.app.miklink.core.data.remote.mikrotik.dto.NeighborDetail
import com.app.miklink.core.data.remote.mikrotik.dto.PingResult
import com.app.miklink.core.data.remote.mikrotik.dto.SpeedTestResult

/**
 * Repository per operazioni MikroTik usate dai test.
 * Incapsula le chiamate REST necessarie per eseguire i vari step di test.
 */
interface MikroTikTestRepository {
    /**
     * Monitora lo stato ethernet di una interfaccia.
     * @param probe Configurazione della probe
     * @param interfaceName Nome dell'interfaccia (es. "ether1")
     * @param once Se true, ritorna immediatamente; se false, continua il monitoraggio
     * @return MonitorResponse con status e rate
     */
    suspend fun monitorEthernet(probe: ProbeConfig, interfaceName: String, once: Boolean = true): MonitorResponse

    /**
     * Esegue cable test (TDR) su una interfaccia.
     * @param probe Configurazione della probe
     * @param interfaceName Nome dell'interfaccia
     * @param once Se true, ritorna immediatamente; se false, continua il test
     * @return CableTestResult con status e cable pairs
     */
    suspend fun cableTest(probe: ProbeConfig, interfaceName: String, once: Boolean = true): CableTestResult

    /**
     * Esegue ping verso un target.
     * @param probe Configurazione della probe
     * @param target Indirizzo IP o hostname
     * @param interfaceName Interfaccia da usare (opzionale)
     * @param count Numero di ping da inviare
     * @return Lista di PingResult (uno per ogni ping)
     */
    suspend fun ping(probe: ProbeConfig, target: String, interfaceName: String?, count: Int): List<PingResult>

    /**
     * Ottiene i neighbor LLDP/CDP per una interfaccia.
     * @param probe Configurazione della probe
     * @param interfaceName Nome dell'interfaccia
     * @return Lista di NeighborDetail
     */
    suspend fun neighbors(probe: ProbeConfig, interfaceName: String): List<NeighborDetail>

    /**
     * Esegue speed test verso un server.
     * @param probe Configurazione della probe
     * @param serverAddress Indirizzo del server speed test
     * @param username Username per autenticazione (opzionale)
     * @param password Password per autenticazione (opzionale)
     * @param duration Durata del test in secondi (default: "5")
     * @return SpeedTestResult con risultati del test
     */
    suspend fun speedTest(
        probe: ProbeConfig,
        serverAddress: String,
        username: String? = null,
        password: String? = null,
        duration: String = "5"
    ): SpeedTestResult

    /**
     * Ottiene le risorse di sistema (per verificare connessione).
     * @param probe Configurazione della probe
     * @return SystemResource (board-name, ecc.)
     */
    suspend fun systemResource(probe: ProbeConfig): com.app.miklink.core.data.remote.mikrotik.dto.SystemResource
}


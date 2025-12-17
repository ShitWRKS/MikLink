package com.app.miklink.core.data.repository.probe

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.data.repository.ProbeCheckResult

/**
 * Repository per verifica connessione e validazione configurazione sonde MikroTik.
 *
 * Esegue chiamate one-shot all'API MikroTik per verificare che le credenziali siano corrette
 * e ottenere informazioni hardware della sonda.
 *
 * Input: ProbeConfig (configurazione sonda da verificare)
 * Output: ProbeCheckResult.Success(boardName, interfaces, effectiveIsHttps, didFallbackToHttp, warning)
 *         oppure ProbeCheckResult.Error(message)
 * Error policy: Gli errori vengono catturati e convertiti in ProbeCheckResult.Error con messaggio
 *               descrittivo. Nessuna eccezione non gestita.
 */
interface ProbeConnectivityRepository {
    /**
     * Verifica la connessione a una sonda MikroTik e ottiene informazioni hardware.
     *
     * Esegue chiamate API per:
     * - Verificare che le credenziali siano corrette
     * - Ottenere il nome del board (board-name)
     * - Ottenere la lista delle interfacce ethernet disponibili
     * - Riportare lo schema di trasporto effettivo (HTTP/HTTPS) e l'eventuale fallback
     *
     * @param probe Configurazione della sonda da verificare
     * @return ProbeCheckResult.Success con boardName e lista interfacce se la connessione e' riuscita,
     *         oppure ProbeCheckResult.Error con messaggio descrittivo in caso di errore
     */
    suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult
}

package com.app.miklink.ui.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.*
import com.app.miklink.data.db.model.*
import com.app.miklink.data.repository.AppRepository
import com.app.miklink.data.repository.AppRepository.NetworkConfigFeedback
import com.app.miklink.utils.UiState
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val clientDao: ClientDao,
    private val probeDao: ProbeConfigDao,
    private val profileDao: TestProfileDao,
    private val reportDao: ReportDao,
    private val repository: AppRepository,
    private val moshi: Moshi
) : ViewModel() {

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log = _log.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<Report>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Persistenza dettagli ping: non vengono resettati al completamento del test
    private val _pingDetails = MutableStateFlow<List<TestDetail>?>(null)
    val pingDetails: StateFlow<List<TestDetail>?> = _pingDetails.asStateFlow()

    // Override per-singolo-test: se non nullo, verrà usato in applyClientNetworkConfig
    var overrideClientNetwork: Client? = null

    private val _sections = MutableStateFlow<List<TestSection>>(emptyList())
    val sections: StateFlow<List<TestSection>> = _sections.asStateFlow()
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // Helper per aggiornare/aggiungere una sezione
    private fun upsertSection(section: TestSection) {
        val current = _sections.value.toMutableList()
        val idx = current.indexOfFirst { it.type == section.type }
        if (idx >= 0) current[idx] = section else current.add(section)
        _sections.value = current
    }

    // init { startTest() } // rimosso: l'avvio è manuale da UI


    fun startTest() {
        // Guard: evita doppio avvio se già in esecuzione
        if (_isRunning.value) {
            addLog("Ignorato startTest(): test già in esecuzione")
            return
        }
        viewModelScope.launch {
            _log.value = emptyList()
            _uiState.value = UiState.Loading
            _sections.value = emptyList()
            _isRunning.value = true

            // Reset ping details at test start (we will repopulate durante il test)
            _pingDetails.value = null

            // Read SavedStateHandle defensively: Nav may pass path segments as Strings
            val clientId: Long = (savedStateHandle.get<Long>("clientId") ?: savedStateHandle.get<String>("clientId")?.toLongOrNull() ?: -1L)
            val probeId: Long = (savedStateHandle.get<Long>("probeId") ?: savedStateHandle.get<String>("probeId")?.toLongOrNull() ?: -1L)
            val profileId: Long = (savedStateHandle.get<Long>("profileId") ?: savedStateHandle.get<String>("profileId")?.toLongOrNull() ?: -1L)
            val socketNameRaw: String = (savedStateHandle.get<String>("socketName") ?: "")

            // Decode URI-encoded socketName
            val socketName = try {
                android.net.Uri.decode(socketNameRaw)
            } catch (_: Exception) {
                socketNameRaw
            }

            // Validate parameters
            if (clientId <= 0 || probeId <= 0 || profileId <= 0) {
                addLog("ERRORE: Parametri di test non validi. client=$clientId probe=$probeId profile=$profileId")
                _uiState.value = UiState.Error("Parametri di navigazione non validi. Impossibile avviare il test.")
                _isRunning.value = false
                return@launch
            }

            val client = clientDao.getClientById(clientId).firstOrNull()
            val probe = probeDao.getProbeById(probeId).firstOrNull()
            val profile = profileDao.getProfileById(profileId).firstOrNull()

            if (client == null || probe == null || profile == null) {
                addLog("ERRORE: Dati di test non trovati nel database. client=$clientId probe=$probeId profile=$profileId socket=$socketName")
                _uiState.value = UiState.Error("Impossibile caricare i dati di test. Cliente, sonda o profilo non esistono.")
                _isRunning.value = false
                return@launch
            }

            val testResults = mutableMapOf<String, Any>()
            var overallStatus = "PASS"

            try {
                addLog("--- INIZIO TEST ---")
                // Sezione Network INFO (placeholder iniziale)
                upsertSection(
                    TestSection(
                        category = TestSectionCategory.INFO,
                        type = TestSectionType.NETWORK,
                        title = "Rete (Client Config)",
                        status = "INFO",
                        details = listOf(TestDetail("Stato", "In corso..."))
                    )
                )

                addLog("Cliente: ${client.companyName} | Presa: $socketName")
                addLog("Sonda '${probe.name}' selezionata.")

                // 1) Applica configurazione rete cliente (persistente) + eventuale override per singolo test
                addLog("Applicazione configurazione rete (${overrideClientNetwork?.networkMode ?: client.networkMode})...")
                when (val apply = repository.applyClientNetworkConfig(probe, client, overrideClientNetwork)) {
                    is UiState.Success -> {
                        val fb = apply.data
                        addLog("Rete: ${fb.mode} ${fb.address ?: ""} gw=${fb.gateway ?: ""}")
                        testResults["network"] = fb
                        upsertSection(
                            TestSection(
                                category = TestSectionCategory.INFO,
                                type = TestSectionType.NETWORK,
                                title = "Rete (Client Config)",
                                status = if ((fb.address ?: fb.gateway) != null) "PASS" else "INFO",
                                details = listOf(
                                    TestDetail("Modalità", fb.mode),
                                    TestDetail("Interfaccia", fb.interfaceName),
                                    TestDetail("Indirizzo", fb.address ?: "-"),
                                    TestDetail("Gateway", fb.gateway ?: "-"),
                                    TestDetail("DNS", fb.dns ?: "-"),
                                    TestDetail("Messaggio", fb.message)
                                )
                            )
                        )
                    }
                    is UiState.Error -> {
                        addLog("Configurazione rete: FALLITA (${apply.message})")
                        overallStatus = "FAIL"
                    }
                    else -> {}
                }

                // 2) TDR
                if (profile.runTdr) {
                    if (probe.tdrSupported) {
                        addLog("Esecuzione TDR (Cable-Test)...")
                        // TDR placeholder
                        upsertSection(
                            TestSection(
                                category = TestSectionCategory.TEST,
                                type = TestSectionType.TDR,
                                title = "TDR (Cable-Test)",
                                status = "INFO",
                                details = listOf(TestDetail("Stato", "In corso..."))
                            )
                        )
                        when (val tdrResult = repository.runCableTest(probe, probe.testInterface)) {
                            is UiState.Success -> {
                                addLog("TDR: SUCCESSO.")
                                testResults["tdr"] = tdrResult.data
                                // Aggiorna sezione TDR con risultati
                                upsertSection(
                                    TestSection(
                                        category = TestSectionCategory.TEST,
                                        type = TestSectionType.TDR,
                                        title = "TDR (Cable-Test)",
                                        status = "PASS",
                                        details = listOf(TestDetail("Risultato", "SUCCESSO"))
                                    )
                                )
                            }
                            is UiState.Error -> {
                                addLog("TDR: FALLITO (${tdrResult.message})")
                                overallStatus = "FAIL"
                                // Aggiorna sezione TDR con errore
                                upsertSection(
                                    TestSection(
                                        category = TestSectionCategory.TEST,
                                        type = TestSectionType.TDR,
                                        title = "TDR (Cable-Test)",
                                        status = "FAIL",
                                        details = listOf(TestDetail("Risultato", "FALLITO"), TestDetail("Motivo", tdrResult.message))
                                    )
                                )
                            }
                            UiState.Idle -> {}
                            UiState.Loading -> {}
                        }
                    } else {
                        addLog("TDR: SALTATO (Sonda '${probe.modelName}' non compatibile)")
                    }
                }

                // 3) Link Status + soglia minLinkRate
                if (profile.runLinkStatus) {
                    addLog("Esecuzione Test Stato Link...")
                    // Link placeholder
                    upsertSection(
                        TestSection(
                            category = TestSectionCategory.TEST,
                            type = TestSectionType.LINK,
                            title = "Link",
                            status = "INFO",
                            details = listOf(TestDetail("Stato", "In corso..."))
                        )
                    )
                    when (val linkResult = repository.getLinkStatus(probe, probe.testInterface)) {
                        is UiState.Success -> {
                            val data = linkResult.data
                            addLog("Stato Link: ${data.status} @ ${data.rate ?: "?"}")
                            testResults["link"] = data

                            // Se il link è down, interrompi il test
                            val isDown = data.status.contains("down", ignoreCase = true)
                            if (isDown) {
                                addLog("ATTENZIONE: Link DOWN rilevato. Test interrotto.")
                                overallStatus = "FAIL"
                                // Aggiorna sezione Link come FAIL
                                upsertSection(
                                    TestSection(
                                        category = TestSectionCategory.TEST,
                                        type = TestSectionType.LINK,
                                        title = "Link",
                                        status = "FAIL",
                                        details = listOf(
                                            TestDetail("Status", data.status),
                                            TestDetail("Rate", data.rate ?: "-")
                                        )
                                    )
                                )
                                finalizeAndEmit(reportClient = client, reportProbe = probe, reportProfile = profile, socketName = socketName, overallStatus = overallStatus, testResults = testResults, notes = "Test interrotto: Link DOWN")
                                return@launch
                            }

                            // Verifica soglia minLinkRate
                            val okRate = isRateOk(data.rate, client.minLinkRate)
                            if (!okRate) {
                                addLog("Velocità link inferiore alla soglia (${client.minLinkRate}) → FAIL")
                                overallStatus = "FAIL"
                            } else {
                                addLog("Velocità link conforme alla soglia minima (${client.minLinkRate}) → PASS")
                            }
                            val linkPass = okRate
                            // Aggiorna sezione Link con risultato puntuale
                            upsertSection(
                                TestSection(
                                    category = TestSectionCategory.TEST,
                                    type = TestSectionType.LINK,
                                    title = "Link",
                                    status = if (linkPass) "PASS" else "FAIL",
                                    details = listOf(
                                        TestDetail("Status", data.status),
                                        TestDetail("Rate", data.rate ?: "-")
                                    )
                                )
                            )
                        }
                        is UiState.Error -> {
                            addLog("Stato Link: FALLITO (${linkResult.message})")
                            overallStatus = "FAIL"
                            upsertSection(
                                TestSection(
                                    category = TestSectionCategory.TEST,
                                    type = TestSectionType.LINK,
                                    title = "Link",
                                    status = "FAIL",
                                    details = listOf(TestDetail("Errore", linkResult.message))
                                )
                            )
                        }
                        UiState.Idle -> {}
                        UiState.Loading -> {}
                    }
                }

                // 4) LLDP/CDP Discovery
                if (profile.runLldp) {
                    addLog("Esecuzione discovery LLDP/CDP...")
                    addLog("DEBUG: LLDP - Interface test: ${probe.testInterface}")
                    upsertSection(
                        TestSection(
                            category = TestSectionCategory.INFO,
                            type = TestSectionType.LLDP,
                            title = "LLDP/CDP",
                            status = "INFO",
                            details = listOf(TestDetail("Stato", "In corso..."))
                        )
                    )
                    when (val lldpResult = repository.getNeighborsForInterface(probe, probe.testInterface)) {
                        is UiState.Success -> {
                            val neighbors = lldpResult.data
                            addLog("DEBUG: LLDP Success - Ricevuti ${neighbors.size} neighbor(s)")
                            if (neighbors.isNotEmpty()) {
                                val neighbor = neighbors.first()
                                addLog("DEBUG: LLDP - Identity: ${neighbor.identity}, Interface: ${neighbor.interfaceName}, Protocol: ${neighbor.discoveredBy}")
                                addLog("LLDP/CDP: Rilevato '${neighbor.identity ?: "Unknown"}' su porta ${neighbor.interfaceName ?: "-"}")
                                testResults["lldp"] = neighbor
                                upsertSection(
                                    TestSection(
                                        category = TestSectionCategory.INFO,
                                        type = TestSectionType.LLDP,
                                        title = "LLDP/CDP",
                                        status = "PASS",
                                        details = listOf(
                                            TestDetail("Identity", neighbor.identity ?: "-"),
                                            TestDetail("Porta", neighbor.interfaceName ?: "-"),
                                            TestDetail("Caps", neighbor.systemCaps ?: "-"),
                                            TestDetail("Protocollo", neighbor.discoveredBy ?: "-")
                                        )
                                    )
                                )
                                addLog("DEBUG: LLDP - upsertSection chiamato con status=PASS e ${4} dettagli")
                            } else {
                                addLog("DEBUG: LLDP - Lista neighbor vuota")
                                addLog("LLDP/CDP: Nessun neighbor rilevato")
                                upsertSection(
                                    TestSection(
                                        category = TestSectionCategory.INFO,
                                        type = TestSectionType.LLDP,
                                        title = "LLDP/CDP",
                                        status = "INFO",
                                        details = listOf(TestDetail("Risultato", "Nessun neighbor rilevato"))
                                    )
                                )
                            }
                        }
                        is UiState.Error -> {
                            addLog("DEBUG: LLDP Error - ${lldpResult.message}")
                            addLog("LLDP/CDP: FALLITO (${lldpResult.message})")
                            upsertSection(
                                TestSection(
                                    category = TestSectionCategory.INFO,
                                    type = TestSectionType.LLDP,
                                    title = "LLDP/CDP",
                                    status = "INFO",
                                    details = listOf(TestDetail("Errore", lldpResult.message))
                                )
                            )
                        }
                        is UiState.Loading -> {
                            addLog("DEBUG: LLDP Loading - NON dovrebbe mai succedere qui!")
                        }
                        is UiState.Idle -> {
                            addLog("DEBUG: LLDP Idle - NON dovrebbe mai succedere qui!")
                        }
                    }
                }

                // 5) Ping Tests
                if (profile.runPing) {
                    // Flag per garantire la card anche nella schermata risultati
                    testResults["ping_configured"] = true
                    val pingTargets = listOfNotNull(profile.pingTarget1, profile.pingTarget2, profile.pingTarget3).filter { it.isNotBlank() }
                    if (pingTargets.isEmpty()) {
                        addLog("Ping: SALTATO (nessun target configurato)")
                        upsertSection(
                            TestSection(
                                category = TestSectionCategory.TEST,
                                type = TestSectionType.PING,
                                title = "Ping",
                                status = "SKIPPED",
                                details = listOf(TestDetail("Motivo", "Nessun target configurato"))
                            )
                        )
                    } else {
                        // Placeholder iniziale per mostrare subito la card
                        upsertSection(
                            TestSection(
                                category = TestSectionCategory.TEST,
                                type = TestSectionType.PING,
                                title = "Ping",
                                status = "INFO",
                                details = listOf(TestDetail("Stato", "In corso..."))
                            )
                        )

                        addLog("Esecuzione Ping verso ${pingTargets.size} target...")
                        val pingDetails = mutableListOf<TestDetail>()
                        var allPingsPassed = true

                        for (target in pingTargets) {
                            val resolvedTarget = try {
                                repository.resolveTargetIp(probe, target, probe.testInterface)
                            } catch (e: Exception) {
                                addLog("Ping $target: Risoluzione fallita (${e.message})")
                                pingDetails.add(TestDetail(target, "Risoluzione fallita"))
                                allPingsPassed = false
                                continue
                            }

                            if (resolvedTarget.equals("DHCP_GATEWAY", ignoreCase = true)) {
                                addLog("Ping $target: SALTATO (gateway DHCP non disponibile)")
                                pingDetails.add(TestDetail(target, "Gateway non risolto"))
                                continue
                            }

                            addLog("Ping verso $resolvedTarget...")
                            when (val pingResult = repository.runPing(probe, resolvedTarget, probe.testInterface, profile.pingCount)) {
                                is UiState.Success -> {
                                    val pingResults = pingResult.data
                                    // Calcola statistiche aggregate
                                    val lastResult = pingResults.lastOrNull()
                                    val avgRtt = lastResult?.avgRtt ?: "N/A"
                                    val packetLoss = lastResult?.packetLoss ?: "N/A"

                                    addLog("Ping $resolvedTarget: SUCCESSO (avg: $avgRtt, loss: $packetLoss%)")
                                    testResults["ping_$target"] = pingResults

                                    // Crea dettagli per ogni ping individuale
                                    val pingDetailsList = mutableListOf<TestDetail>()
                                    pingDetailsList.add(TestDetail("Target", "$target → $resolvedTarget"))
                                    pingDetailsList.add(TestDetail("Pacchetti inviati", pingResults.size.toString()))
                                    pingDetailsList.add(TestDetail("Packet Loss", "${packetLoss}%"))
                                    pingDetailsList.add(TestDetail("Avg RTT", avgRtt))
                                    pingDetailsList.add(TestDetail("Min RTT", lastResult?.minRtt ?: "N/A"))
                                    pingDetailsList.add(TestDetail("Max RTT", lastResult?.maxRtt ?: "N/A"))
                                    pingDetailsList.add(TestDetail("---", "Dettaglio ping individuali:"))

                                    pingResults.forEach { ping ->
                                        pingDetailsList.add(
                                            TestDetail(
                                                "Ping #${ping.seq ?: "?"}",
                                                "time=${ping.time ?: "N/A"} ttl=${ping.ttl ?: "N/A"}"
                                            )
                                        )
                                    }

                                    pingDetails.addAll(pingDetailsList)

                                    // Determina se questo specifico target ha avuto successo
                                    val thisTargetPassed = try {
                                        val numeric = packetLoss.filter { it.isDigit() || it == '.' }
                                        if (numeric.isBlank()) false else (numeric.toDoubleOrNull() ?: Double.MAX_VALUE) <= 0.0
                                    } catch (_: Exception) { false }

                                    if (!thisTargetPassed) allPingsPassed = false
                                }
                                is UiState.Error -> {
                                    addLog("Ping $resolvedTarget: FALLITO (${pingResult.message})")
                                    pingDetails.add(TestDetail(target, "FAIL: ${pingResult.message}"))
                                    allPingsPassed = false
                                }
                                else -> {}
                            }
                        }

                        val pingStatus = if (allPingsPassed && pingDetails.isNotEmpty()) "PASS" else if (pingDetails.isEmpty()) "SKIPPED" else "FAIL"

                        // Salva i dettagli ping nello stato dedicato prima di upsertSection per persistenza
                        _pingDetails.value = pingDetails.toList()

                        upsertSection(
                            TestSection(
                                category = TestSectionCategory.TEST,
                                type = TestSectionType.PING,
                                title = "Ping",
                                status = pingStatus,
                                details = pingDetails
                            )
                        )
                        if (!allPingsPassed) overallStatus = "FAIL"
                    }
                }


            } catch (e: Exception) {
                addLog("ERRORE IRREVERSIBILE: ${e.message}")
                overallStatus = "FAIL"
            }

            addLog("--- TEST COMPLETATO ---")
            _isRunning.value = false
            finalizeAndEmit(
                reportClient = client, reportProbe = probe, reportProfile = profile, socketName = socketName,
                overallStatus = overallStatus, testResults = testResults, notes = null
            )
        }
    }

    /**
     * Visibilità `internal` per permettere il test unitario diretto.
     * Controlla se il rate fornito (post-parsing) supera la soglia minima del profilo.
     */
    internal fun isRateOk(rate: String?, min: String): Boolean {
        // Use RateParser to normalize both values to Mbps
        val actual = com.app.miklink.utils.RateParser.parseToMbps(rate)
        val threshold = com.app.miklink.utils.RateParser.parseToMbps(min)

        // Se rate è nullo o non riconosciuto (parsato come 0), FAIL
        if (actual == 0 && !rate.isNullOrBlank()) {
            addLog("ATTENZIONE: Formato velocità non riconosciuto ('$rate') → FAIL")
        } else if (rate.isNullOrBlank()) {
            addLog("ATTENZIONE: Velocità non disponibile → FAIL")
        }

        return actual >= threshold
    }

    private fun finalizeAndEmit(
        reportClient: Client,
        reportProbe: ProbeConfig,
        reportProfile: TestProfile,
        socketName: String,
        overallStatus: String,
        testResults: Map<String, Any>,
        notes: String?
    ) {
        // NON ricostruire le sezioni - le sezioni create con upsertSection durante il test
        // sono GIÀ CORRETTE e rappresentano lo stato reale.
        // Ricostruirle causa inconsistenze tra "test in corso" e "risultati finali".

        val type: Type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter: JsonAdapter<Map<String, Any>> = moshi.adapter(type)
        val resultsJson = adapter.toJson(testResults)

        val report = Report(
            clientId = reportClient.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = socketName,
            notes = notes,
            probeName = reportProbe.name,
            profileName = reportProfile.profileName,
            overallStatus = overallStatus,
            resultsJson = resultsJson
        )

        _uiState.value = UiState.Success(report)
    }


    fun saveReportToDb(report: Report) {
        viewModelScope.launch {
            reportDao.insert(report)
            report.clientId?.let {
                clientDao.incrementNextIdNumber(it)
            }
        }
    }

    private fun addLog(message: String) {
        _log.value = _log.value + message
    }
}
package com.app.miklink.ui.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.local.room.v1.dao.*
import com.app.miklink.core.data.local.room.v1.model.*
import com.app.miklink.core.data.remote.mikrotik.dto.SpeedTestResult
import com.app.miklink.core.data.repository.AppRepository
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.utils.UiState
import com.app.miklink.utils.normalizeTime
import com.app.miklink.utils.normalizeLinkSpeed
import com.app.miklink.utils.normalizeLinkStatus
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.catch
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
    private val repository: AppRepository, // TODO: Rimuovere quando completamente migrato
    private val moshi: Moshi,
    private val runTestUseCase: RunTestUseCase
) : ViewModel() {

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log = _log.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<Report>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Persistenza dettagli ping: non vengono resettati al completamento del test
    @Suppress("unused") // exposed to UI (composables) — not read inside this file
    private val _pingDetails = MutableStateFlow<List<TestDetail>?>(null)
    @Suppress("unused") // observed by UI; keep public for composables
    val pingDetails: StateFlow<List<TestDetail>?> = _pingDetails.asStateFlow()

    // Speed Test state
    @Suppress("unused") // observed by UI; keep public for composables
    private val _speedTestState = MutableStateFlow<UiState<SpeedTestResult>>(UiState.Idle)
    @Suppress("unused")
    val speedTestState: StateFlow<UiState<SpeedTestResult>> = _speedTestState.asStateFlow()

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

            // S5: Usa RunTestUseCase invece di orchestrare direttamente
            val plan = TestPlan(
                clientId = clientId,
                probeId = probeId,
                profileId = profileId,
                socketId = socketName,
                notes = null
            )

            // Colleziona eventi dal UseCase e mappa allo stato UI
            runTestUseCase.execute(plan)
                .catch { e ->
                    addLog("ERRORE IRREVERSIBILE: ${e.message}")
                    _isRunning.value = false
                    _uiState.value = UiState.Error(e.message ?: "Errore sconosciuto")
                }
                .collect { event ->
                    when (event) {
                        is TestEvent.LogLine -> {
                            addLog(event.message)
                        }
                        is TestEvent.Progress -> {
                            // TODO: Aggiornare progress UI se necessario
                        }
                        is TestEvent.Completed -> {
                            _isRunning.value = false
                            // TODO: Convertire TestOutcome in Report e salvare
                            // Per ora manteniamo compatibilità con codice esistente
                            val client = clientDao.getClientById(clientId).firstOrNull()
                            val profile = profileDao.getProfileById(profileId).firstOrNull()
                            if (client != null && profile != null) {
                                finalizeAndEmitFromOutcome(
                                    reportClient = client,
                                    reportProfile = profile,
                                    socketName = socketName,
                                    outcome = event.outcome
                                )
                            }
                        }
                        is TestEvent.Failed -> {
                            _isRunning.value = false
                            _uiState.value = UiState.Error(event.error.message)
                        }
                    }
                }

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

    /**
     * Finalizza e emette Report da TestOutcome (S5: nuovo flusso UseCase).
     */
    private fun finalizeAndEmitFromOutcome(
        reportClient: Client,
        reportProfile: TestProfile,
        socketName: String,
        outcome: com.app.miklink.core.domain.test.model.TestOutcome
    ) {
        val resultsJson = outcome.rawResultsJson ?: "{}"
        val report = Report(
            clientId = reportClient.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = socketName,
            notes = null,
            probeName = "Sonda",
            profileName = reportProfile.profileName,
            overallStatus = outcome.overallStatus,
            resultsJson = resultsJson
        )
        _uiState.value = UiState.Success(report)
    }

    private fun finalizeAndEmit(
        reportClient: Client,
        reportProfile: TestProfile,
        socketName: String,
        overallStatus: String,
        testResults: Map<String, Any>,
        notes: String? = null
    ) {
        // NON ricostruire le sezioni - le sezioni create con upsertSection durante il test
        // sono GIÀ CORRETTE e rappresentano lo stato reale.
        // Ricostruirle causa inconsistenze tra "test in corso" e "risultati finali".

        // Normalizzazione schema per compatibilità con PdfGenerator/ParsedResults
        val normalizedResults = java.util.LinkedHashMap<String, Any>(testResults)
        try {
            // 1) Consolidamento Ping: raccogli tutte le chiavi "ping_*" in una singola lista "ping"
            val combinedPing = mutableListOf<com.app.miklink.core.data.remote.mikrotik.dto.PingResult>()
            testResults.forEach { (k, v) ->
                if (k.startsWith("ping_")) {
                    val list = v as? List<*>
                    list?.forEach { item ->
                        (item as? com.app.miklink.core.data.remote.mikrotik.dto.PingResult)?.let { combinedPing.add(it) }
                    }
                }
            }
            if (combinedPing.isNotEmpty()) {
                normalizedResults["ping"] = combinedPing
            }

            // 2) TDR: se presente come oggetto singolo, wrappa in lista
            when (val tdrVal = testResults["tdr"]) {
                is com.app.miklink.core.data.remote.mikrotik.dto.CableTestResult -> {
                    normalizedResults["tdr"] = listOf(tdrVal)
                }
                is List<*> -> {
                    // Già lista: niente da fare
                }
            }
        } catch (e: Exception) {
            addLog("ATTENZIONE: Normalizzazione risultati non riuscita (${e.message}).")
        }

        val resultsJson: String = try {
            val type: Type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter: JsonAdapter<Map<String, Any>> = moshi.adapter(type)
            adapter.toJson(normalizedResults)
        } catch (e: Exception) {
            addLog("ATTENZIONE: Serializzazione risultati fallita (${e.message}). Uso JSON vuoto.")
            "{}"
        }

        val report = Report(
            clientId = reportClient.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = socketName,
            notes = notes,
            // probe.name removed — use generic label for reports
            probeName = "Sonda",
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

    private fun emitImmediateFail(notes: String) {
        _uiState.value = UiState.Error(notes)
    }
}
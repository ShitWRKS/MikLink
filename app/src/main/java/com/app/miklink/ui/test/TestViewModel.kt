package com.app.miklink.ui.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.*
import com.app.miklink.data.db.model.*
import com.app.miklink.data.repository.AppRepository
import com.app.miklink.utils.UiState
import com.app.miklink.utils.findDirectlyConnectedSwitch
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _uiState = MutableStateFlow<UiState<Report>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        startTest()
    }

    fun startTest() {
        viewModelScope.launch {
            _log.value = emptyList()
            _uiState.value = UiState.Loading

            val clientId: Long = savedStateHandle.get("clientId") ?: -1
            val probeId: Long = savedStateHandle.get("probeId") ?: -1
            val profileId: Long = savedStateHandle.get("profileId") ?: -1
            val socketName: String = savedStateHandle.get("socketName") ?: ""

            val client = clientDao.getClientById(clientId).firstOrNull()
            val probe = probeDao.getProbeById(probeId).firstOrNull()
            val profile = profileDao.getProfileById(profileId).firstOrNull()

            if (client == null || probe == null || profile == null) {
                addLog("ERRORE: Dati di test non validi.")
                _uiState.value = UiState.Error("Could not load test data.")
                return@launch
            }

            val testResults = mutableMapOf<String, Any>()
            var overallStatus = "PASS"
            val vlanId: String? = null
            val ipId: String? = null

            try {
                addLog("--- INIZIO TEST ---")
                addLog("Cliente: ${client.companyName} | Presa: $socketName")
                addLog("Sonda '${probe.name}' selezionata.")

                // Imposta la probe corrente nel repository
                repository.setProbe(probe)

                if (profile.runTdr) {
                    if (probe.tdrSupported) {
                        addLog("Esecuzione TDR (Cable-Test)...")
                        when (val tdrResult = repository.runCableTest(probe.testInterface)) {
                            is UiState.Success -> {
                                addLog("TDR: SUCCESSO.")
                                testResults["tdr"] = tdrResult.data
                            }
                            is UiState.Error -> {
                                addLog("TDR: FALLITO (${tdrResult.message})")
                                overallStatus = "FAIL"
                            }
                            UiState.Loading -> {}
                        }
                    } else {
                        addLog("TDR: SALTATO (Sonda '${probe.modelName}' non compatibile)")
                    }
                }

                if (profile.runLinkStatus) {
                    addLog("Esecuzione Test Stato Link...")
                    when (val linkResult = repository.getLinkStatus(probe.testInterface)) {
                        is UiState.Success -> {
                            val data = linkResult.data
                            addLog("Stato Link: SUCCESSO (${data.status} @ ${data.rate})")
                            testResults["link"] = data

                            // Se il link è down, interrompi il test
                            if (data.status.contains("down", ignoreCase = true)) {
                                addLog("ATTENZIONE: Link DOWN rilevato. Test interrotto.")
                                overallStatus = "FAIL"
                                addLog("--- TEST COMPLETATO ---")

                                val type: Type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                                val adapter: JsonAdapter<Map<String, Any>> = moshi.adapter(type)
                                val resultsJson = adapter.toJson(testResults)

                                val report = Report(
                                    clientId = clientId,
                                    timestamp = System.currentTimeMillis(),
                                    socketName = socketName,
                                    probeName = probe.name,
                                    profileName = profile.profileName,
                                    overallStatus = overallStatus,
                                    resultsJson = resultsJson,
                                    floor = client.lastFloor,
                                    room = client.lastRoom,
                                    notes = "Test interrotto: Link DOWN"
                                )

                                _uiState.value = UiState.Success(report)
                                return@launch
                            }
                        }
                        is UiState.Error -> {
                            addLog("Stato Link: FALLITO (${linkResult.message})")
                            overallStatus = "FAIL"
                        }
                        UiState.Loading -> {}
                    }
                }

                if (profile.runLldp) {
                    addLog("Esecuzione Test LLDP/CDP...")
                    when (val neighborResult = repository.getNeighborsForInterface(probe.testInterface)) {
                        is UiState.Success -> {
                            val switch = findDirectlyConnectedSwitch(neighborResult.data)
                            if (switch != null) {
                                addLog("LLDP: SUCCESSO (Switch: ${switch.identity}, Porta: ${switch.interfaceName})")
                                testResults["lldp"] = switch
                            } else {
                                addLog("LLDP: NESSUN RISULTATO (Nessuno switch 'bridge' trovato)")
                            }
                        }
                        is UiState.Error -> {
                            addLog("LLDP: FALLITO (${neighborResult.message})")
                            overallStatus = "FAIL"
                        }
                        UiState.Loading -> {}
                    }
                }

                if (profile.runPing) {
                    addLog("Esecuzione Test Ping...")
                    val targets = listOfNotNull(profile.pingTarget1, profile.pingTarget2, profile.pingTarget3).filter { it.isNotBlank() }
                    targets.forEach { target ->
                        val resolvedTarget = if (target.equals("DHCP_GATEWAY", ignoreCase = true)) {
                            repository.getDhcpGateway(probe.testInterface) ?: target
                        } else {
                            target
                        }

                        when (val pingResult = repository.runPing(resolvedTarget, probe.testInterface)) {
                            is UiState.Success -> {
                                val avgRtt = pingResult.data.avgRtt
                                if (!avgRtt.isNullOrBlank() && avgRtt.toDoubleOrNull() != null && avgRtt.toDouble() > 0) {
                                    addLog("Ping ($resolvedTarget): SUCCESSO (${avgRtt}ms)")
                                    testResults["ping_$resolvedTarget"] = pingResult.data
                                } else {
                                    addLog("Ping ($resolvedTarget): FALLITO (Nessuna risposta)")
                                    overallStatus = "FAIL"
                                }
                            }
                            is UiState.Error -> {
                                addLog("Ping ($resolvedTarget): FALLITO (${pingResult.message})")
                                overallStatus = "FAIL"
                            }
                            UiState.Loading -> {}
                        }
                    }
                }

            } catch (e: Exception) {
                addLog("ERRORE IRREVERSIBILE: ${e.message}")
                overallStatus = "FAIL"
            } finally {
                addLog("--- FASE 4: PULIZIA FINALE ---")
                vlanId?.let {
                    addLog("Rimozione VLAN ($it)...")
                    repository.removeVlan(it)
                }
                ipId?.let {
                    addLog("Rimozione IP ($it)...")
                    repository.removeIpAddress(it)
                }

                addLog("--- TEST COMPLETATO ---")
            }

            val type: Type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter: JsonAdapter<Map<String, Any>> = moshi.adapter(type)
            val resultsJson = adapter.toJson(testResults)

            val report = Report(
                clientId = clientId,
                timestamp = System.currentTimeMillis(),
                socketName = socketName,
                probeName = probe.name,
                profileName = profile.profileName,
                overallStatus = overallStatus,
                resultsJson = resultsJson,
                floor = client.lastFloor,
                room = client.lastRoom,
                notes = null
            )

            _uiState.value = UiState.Success(report)
        }
    }

    fun saveReportToDb(report: Report) {
        viewModelScope.launch {
            reportDao.insert(report)
            report.clientId?.let {
                clientDao.updateNextIdAndStickyFields(it, report.floor, report.room)
            }
        }
    }

    private fun addLog(message: String) {
        _log.value = _log.value + message
    }
}
/*
 * Purpose: Provide dashboard state (clients, probe status, profiles) and deterministic socket name suggestions.
 * Inputs: Client/test profile/probe repositories, report history, probe status stream, user preferences, and id numbering strategy.
 * Outputs: StateFlows for UI (clients, profiles, probe, online state, socket suggestion, glow intensity) plus gap-filling socket id helper.
 * Notes: Uses SocketIdLite policy for formatting/parsing to align dashboard suggestions with ADR-0004 and keep logic pure inside ViewModel.
 */
package com.app.miklink.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.socketNameFor
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.data.repository.probe.ProbeStatusRepository
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.policy.socketid.SocketIdLite
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import com.app.miklink.core.domain.usecase.preferences.ObserveIdNumberingStrategyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    clientRepository: ClientRepository,
    testProfileRepository: TestProfileRepository,
    private val reportRepository: ReportRepository,
    probeRepository: ProbeRepository,
    private val probeStatusRepository: ProbeStatusRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    observeIdNumberingStrategyUseCase: ObserveIdNumberingStrategyUseCase
) : ViewModel() {

    // Data sources
    val clients: StateFlow<List<Client>> = clientRepository.observeAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sonda unica (singleton)
    val currentProbe: StateFlow<ProbeConfig?> = probeRepository.observeProbeConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<TestProfile>> = testProfileRepository.observeAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User selections
    val selectedClient = MutableStateFlow<Client?>(null)
    // RIMOSSO: val selectedProbe (ora usa currentProbe)
    val selectedProfile = MutableStateFlow<TestProfile?>(null)
    val socketName = MutableStateFlow("")

    val isProbeOnline: StateFlow<Boolean> = currentProbe.flatMapLatest { probe ->
        if (probe == null) {
            flowOf(false)
        } else {
            probeStatusRepository.observeProbeStatus(probe)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Osserva la strategia di numerazione ID
    private val idNumberingStrategy = observeIdNumberingStrategyUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IdNumberingStrategy.CONTINUOUS_INCREMENT)

    val dashboardGlowIntensity = userPreferencesRepository.dashboardGlowIntensity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        )

    init {
        // Combina selectedClient e idNumberingStrategy per calcolare socketName
        viewModelScope.launch {
            combine(selectedClient, idNumberingStrategy) { client, strategy ->
                Pair(client, strategy)
            }.collect { (client, strategy) ->
                if (client != null) {
                    val nextNumber = when (strategy) {
                        IdNumberingStrategy.CONTINUOUS_INCREMENT -> {
                            // Usa sempre nextIdNumber (auto-increment continuo)
                            client.nextIdNumber
                        }
                        IdNumberingStrategy.FILL_GAPS -> {
                            // Trova il primo gap disponibile o usa nextIdNumber
                            findNextAvailableId(client)
                        }
                    }
                    // Format using domain helper to keep generation deterministic
                    socketName.value = client.socketNameFor(nextNumber)
                } else {
                    socketName.value = ""
                }
            }
        }
    }

    // Funzione helper per trovare il primo ID disponibile (gap-filling)
    private suspend fun findNextAvailableId(client: Client): Int {
        val existingReports = reportRepository.observeReportsByClient(client.clientId).firstOrNull() ?: emptyList()

        val existingIds = existingReports.mapNotNull { report ->
            val socketName = report.socketName ?: return@mapNotNull null
            SocketIdLite.parseIdNumber(socketName, client.socketPrefix, client.socketSeparator)
        }
        return SocketIdLite.firstMissingPositive(existingIds)
    }
}

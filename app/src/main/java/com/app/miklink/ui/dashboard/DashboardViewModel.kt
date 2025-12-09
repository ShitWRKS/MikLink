package com.app.miklink.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.TestProfile
import com.app.miklink.data.repository.AppRepository
import com.app.miklink.data.repository.IdNumberingStrategy
import com.app.miklink.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    clientDao: ClientDao,
    testProfileDao: TestProfileDao,
    private val reportDao: ReportDao,
    private val repository: AppRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Data sources
    val clients: StateFlow<List<Client>> = clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // MODIFICATO: sonda unica invece di lista
    val currentProbe: StateFlow<ProbeConfig?> = repository.currentProbe
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<TestProfile>> = testProfileDao.getAllProfiles()
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
            repository.observeProbeStatus(probe)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Osserva la strategia di numerazione ID
    private val idNumberingStrategy = userPreferencesRepository.idNumberingStrategy
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
                    // Format: prefix + separator + paddedNumber + separator + suffix
                    val paddedNumber = String.format(Locale.US, "%0${client.socketNumberPadding}d", nextNumber)
                    socketName.value = "${client.socketPrefix}${client.socketSeparator}${paddedNumber}${client.socketSeparator}${client.socketSuffix}"
                } else {
                    socketName.value = ""
                }
            }
        }
    }

    // Funzione helper per trovare il primo ID disponibile (gap-filling)
    private suspend fun findNextAvailableId(client: Client): Int {
        val existingReports = reportDao.getReportsForClient(client.clientId).firstOrNull() ?: emptyList()
        
        // Estrai tutti i numeri ID esistenti
        val existingIds = existingReports.mapNotNull { report ->
            report.socketName?.removePrefix(client.socketPrefix)?.toIntOrNull()
        }.sorted()

        // Trova il primo gap
        var expectedId = 1
        for (existingId in existingIds) {
            if (existingId != expectedId) {
                // Trovato un gap
                return expectedId
            }
            expectedId++
        }

        // Nessun gap trovato, usa il prossimo numero dopo l'ultimo
        return existingIds.lastOrNull()?.plus(1) ?: 1
    }
}
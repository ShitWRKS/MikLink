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
    private val repository: AppRepository
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

    init {
        // Observe selected client to auto-increment socket ID
        viewModelScope.launch {
            selectedClient.collect { client ->
                if (client != null) {
                    val lastReport = reportDao.getLastReportForClient(client.clientId)
                    val nextNumber = if (lastReport == null) {
                        1
                    } else {
                        // Safely handle nullable socketName
                        val lastNumber = lastReport.socketName?.removePrefix(client.socketPrefix)?.toIntOrNull() ?: 0
                        lastNumber + 1
                    }
                    // Format with 3-digit padding
                    socketName.value = "${client.socketPrefix}${String.format(Locale.US, "%03d", nextNumber)}"
                } else {
                    socketName.value = ""
                }
            }
        }
    }
}
package com.app.miklink.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.TestProfile
import com.app.miklink.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    clientDao: ClientDao,
    probeConfigDao: ProbeConfigDao,
    testProfileDao: TestProfileDao,
    private val repository: AppRepository
) : ViewModel() {

    // Data sources
    val clients: StateFlow<List<Client>> = clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val probes: StateFlow<List<ProbeConfig>> = probeConfigDao.getAllProbes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<TestProfile>> = testProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User selections
    val selectedClient = MutableStateFlow<Client?>(null)
    val selectedProfile = MutableStateFlow<TestProfile?>(null)
    val socketName = MutableStateFlow("")

    private val _isProbeOnline = MutableStateFlow(false)
    val isProbeOnline = _isProbeOnline.asStateFlow()

    private var probeObserverJob: Job? = null

    val selectedProbe = MutableStateFlow<ProbeConfig?>(null).also { flow ->
        flow.onEach { probe ->
            probeObserverJob?.cancel()
            if (probe != null) {
                probeObserverJob = repository.observeProbeStatus(probe)
                    .onEach { isOnline -> _isProbeOnline.value = isOnline }
                    .launchIn(viewModelScope)
            } else {
                _isProbeOnline.value = false
            }
        }.launchIn(viewModelScope)
    }
}

package com.app.miklink.ui.probe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProbeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    val allProbes: StateFlow<List<ProbeConfig>> = repository.probeConfigDao.getAllProbes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveProbe(probe: ProbeConfig) {
        viewModelScope.launch {
            repository.probeConfigDao.insert(probe)
        }
    }

    fun deleteProbe(probe: ProbeConfig) {
        viewModelScope.launch {
            repository.probeConfigDao.delete(probe)
        }
    }

    // The logic for checkProbeConnection will be called from the UI
    // and the result will be handled there to update the UI state accordingly.
    // We will expose a function here to trigger it.
    suspend fun checkAndVerifyProbe(probe: ProbeConfig) = repository.checkProbeConnection(probe)
}
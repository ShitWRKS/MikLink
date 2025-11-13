package com.app.miklink.ui.probe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.model.ProbeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProbeListViewModel @Inject constructor(
    private val probeConfigDao: ProbeConfigDao
) : ViewModel() {

    val probes: StateFlow<List<ProbeConfig>> = probeConfigDao.getAllProbes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

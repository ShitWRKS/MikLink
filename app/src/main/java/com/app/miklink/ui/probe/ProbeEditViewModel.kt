package com.app.miklink.ui.probe

import androidx.lifecycle.SavedStateHandle
import com.app.miklink.ui.common.BaseEditViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.core.data.repository.AppRepository
import com.app.miklink.core.data.repository.ProbeCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProbeEditViewModel @Inject constructor(
    private val probeConfigDao: ProbeConfigDao,
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle
) : BaseEditViewModel(savedStateHandle, "probeId") {

    private val probeId: Long = savedStateHandle.get<Long>("probeId") ?: -1L

    val ipAddress = MutableStateFlow("")
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val isHttps = MutableStateFlow(false)
    val testInterface = MutableStateFlow("")

    private val _modelName = MutableStateFlow<String?>(null)
    private val _isOnline = MutableStateFlow(false)
    private val _tdrSupported = MutableStateFlow(false)

    // Internal verification state (used to update UI verification progress/result)
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)

    val verificationState = _verificationState.asStateFlow()

    // isSaved & isEditing handled by BaseEditViewModel

    // Combine connection-related fields into a single flow
    private val connectionDetailsFlow = combine(
        ipAddress,
        username,
        password,
        isHttps
    ) { ip, user, pass, https ->
        ProbeConfig(
            probeId = 0L, // Default value for a temporary verification object
            ipAddress = ip,
            username = user,
            password = pass,
            isHttps = https,
            testInterface = "", // Default value
            isOnline = false, // Default value
            modelName = null, // Default value
            tdrSupported = false // Default value
        )
    }
    init {
        // NUOVO: Carica sonda unica se esiste (navigazione da settings)
        viewModelScope.launch {
            if (!isEditing) {
                probeConfigDao.getSingleProbe().firstOrNull()?.let { probe ->
                    // name was removed from ProbeConfig — not tracked in UI
                    ipAddress.value = probe.ipAddress
                    username.value = probe.username
                    password.value = probe.password
                    isHttps.value = probe.isHttps
                    testInterface.value = probe.testInterface
                    _modelName.value = probe.modelName
                    _tdrSupported.value = probe.tdrSupported
                    _isOnline.value = probe.isOnline
                    if (probe.modelName != null) {
                        _verificationState.value = VerificationState.Success(probe.modelName, listOfNotNull(probe.testInterface))
                    }
                }
            }
        }

        // When connection details change, reset verification state
        viewModelScope.launch {
            connectionDetailsFlow
                .drop(1) // Ignore initial value
                .collect {
                    if (_verificationState.value is VerificationState.Success) {
                        _verificationState.value = VerificationState.Error("Probe details changed. Please verify again.")
                    }
                }
        }
    }

    init {
        // Trigger loading when editing
        loadIfEditing()
    }

    override suspend fun loadEntity(id: Long) {
        probeConfigDao.getProbeById(id).firstOrNull()?.let { probe ->
            ipAddress.value = probe.ipAddress
            username.value = probe.username
            password.value = probe.password
            isHttps.value = probe.isHttps
            testInterface.value = probe.testInterface
            _verificationState.value = VerificationState.Idle
        }
    }

    fun onVerifyClicked() {
        viewModelScope.launch {
            _verificationState.value = VerificationState.Loading
            val tempProbe = connectionDetailsFlow.first()

            when (val result = repository.checkProbeConnection(tempProbe)) {
                is ProbeCheckResult.Success -> {
                    _isOnline.value = true
                    testInterface.value = result.interfaces.firstOrNull() ?: ""
                    _verificationState.value = VerificationState.Success(result.boardName, result.interfaces)
                }
                is ProbeCheckResult.Error -> {
                    _isOnline.value = false
                    _verificationState.value = VerificationState.Error(result.message)
                }
            }
        }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            val probeToSave = ProbeConfig(
                probeId = if (isEditing) probeId else 1, // force ID=1 for single-probe
                ipAddress = ipAddress.value,
                username = username.value,
                password = password.value,
                isHttps = isHttps.value,
                testInterface = testInterface.value,
                isOnline = _isOnline.value,
                modelName = _modelName.value,
                tdrSupported = _tdrSupported.value
            )
            // MODIFICATO: usa upsertSingle per sonda unica
            probeConfigDao.upsertSingle(probeToSave)
            markSaved()
        }
    }
}

sealed class VerificationState {
    object Idle : VerificationState()
    object Loading : VerificationState()
    data class Success(val boardName: String?, val interfaces: List<String>) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

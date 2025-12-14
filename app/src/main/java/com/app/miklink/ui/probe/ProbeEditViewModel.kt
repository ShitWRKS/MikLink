package com.app.miklink.ui.probe

import androidx.lifecycle.SavedStateHandle
import com.app.miklink.ui.common.BaseEditViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.data.repository.ProbeCheckResult
import com.app.miklink.core.data.repository.probe.ProbeConnectivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProbeEditViewModel @Inject constructor(
    private val probeRepository: ProbeRepository,
    private val probeConnectivityRepository: ProbeConnectivityRepository,
    savedStateHandle: SavedStateHandle
) : BaseEditViewModel(savedStateHandle, idKey = null) {  // null = no ID, singleton probe

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
        // Carica sonda unica se esiste (singleton)
        viewModelScope.launch {
            probeRepository.getProbeConfig()?.let { probe ->
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
        // Non serve più loadIfEditing perché è singleton
    }

    override suspend fun loadEntity(id: Long) {
        // Singleton: carica direttamente senza ID
        probeRepository.getProbeConfig()?.let { probe ->
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

            when (val result = probeConnectivityRepository.checkProbeConnection(tempProbe)) {
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
                ipAddress = ipAddress.value,
                username = username.value,
                password = password.value,
                isHttps = isHttps.value,
                testInterface = testInterface.value,
                isOnline = _isOnline.value,
                modelName = _modelName.value,
                tdrSupported = _tdrSupported.value
            )
            probeRepository.saveProbeConfig(probeToSave)
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

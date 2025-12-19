/*
 * Purpose: Manage probe edit/verification state and persist probe configuration including board/model metadata.
 * Inputs: ProbeRepository for persistence, ProbeConnectivityRepository for verification, navigation args via SavedStateHandle.
 * Outputs: Form state flows, verification state, and saved ProbeConfig with modelName/testInterface populated after verification.
 * Notes: _modelName is updated on successful verification to ensure persistence; verificationState is UI-only feedback.
 */
package com.app.miklink.ui.probe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.ProbeCheckResult
import com.app.miklink.core.data.repository.probe.ProbeConnectivityRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.ui.common.BaseEditViewModel
import com.app.miklink.utils.Compatibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
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
    private var suppressVerificationReset = false
    private var lastVerifiedConnection: ProbeConfig? = null

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
                    _verificationState.value = VerificationState.Success(
                        boardName = probe.modelName,
                        interfaces = listOfNotNull(probe.testInterface),
                        didFallbackToHttp = false,
                        warning = null
                    )
                }
            }
        }

        // When connection details change, reset verification state
        viewModelScope.launch {
            connectionDetailsFlow
                .drop(1) // Ignore initial value
                .collect { currentConfig ->
                    if (!suppressVerificationReset && _verificationState.value is VerificationState.Success) {
                        if (lastVerifiedConnection != null && lastVerifiedConnection != currentConfig) {
                            _verificationState.value = VerificationState.Error("Probe details changed. Please verify again.")
                        }
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
            suppressVerificationReset = true
            _verificationState.value = VerificationState.Loading
            val tempProbe = connectionDetailsFlow.first()

            when (val result = probeConnectivityRepository.checkProbeConnection(tempProbe)) {
                is ProbeCheckResult.Success -> {
                    // Sync scheme with effective transport to reflect fallback or HTTPS success.
                    isHttps.value = result.effectiveIsHttps
                    _isOnline.value = true
                    _tdrSupported.value = Compatibility.isTdrSupported(result.boardName)
                    _modelName.value = result.boardName
                    testInterface.value = result.interfaces.firstOrNull() ?: ""
                    lastVerifiedConnection = tempProbe.copy(isHttps = result.effectiveIsHttps)
                    _verificationState.value = VerificationState.Success(
                        boardName = result.boardName,
                        interfaces = result.interfaces,
                        didFallbackToHttp = result.didFallbackToHttp,
                        warning = result.warning
                    )
                }
                is ProbeCheckResult.Error -> {
                    _isOnline.value = false
                    lastVerifiedConnection = null
                    _verificationState.value = VerificationState.Error(result.message)
                }
            }
            suppressVerificationReset = false
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
    data class Success(
        val boardName: String?,
        val interfaces: List<String>,
        val didFallbackToHttp: Boolean,
        val warning: String?
    ) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

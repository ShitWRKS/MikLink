package com.app.miklink.ui.probe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.repository.AppRepository
import com.app.miklink.data.repository.ProbeCheckResult
import com.app.miklink.utils.Compatibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProbeEditViewModel @Inject constructor(
    private val probeConfigDao: ProbeConfigDao,
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val probeId: Long = savedStateHandle.get<Long>("probeId") ?: -1L

    val name = MutableStateFlow("")
    val ipAddress = MutableStateFlow("")
    val username = MutableStateFlow("admin")
    val password = MutableStateFlow("")
    val isHttps = MutableStateFlow(false)
    val testInterface = MutableStateFlow("")

    private val _modelName = MutableStateFlow<String?>(null)
    val modelName = _modelName.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    private val _tdrSupported = MutableStateFlow(false)
    val tdrSupported = _tdrSupported.asStateFlow()

    private val _availableInterfaces = MutableStateFlow<List<String>>(emptyList())
    val availableInterfaces = _availableInterfaces.asStateFlow()

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState = _verificationState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    val isEditing = probeId != -1L

    init {
        if (isEditing) {
            viewModelScope.launch {
                // Correctly fetch the single item from the Flow
                probeConfigDao.getProbeById(probeId).firstOrNull()?.let { probe ->
                    name.value = probe.name
                    ipAddress.value = probe.ipAddress
                    username.value = probe.username
                    password.value = probe.password
                    testInterface.value = probe.testInterface
                    isHttps.value = probe.isHttps
                    _modelName.value = probe.modelName
                    _isOnline.value = probe.isOnline
                    _tdrSupported.value = probe.tdrSupported
                    _verificationState.value = VerificationState.Success(probe.modelName, listOf(probe.testInterface))
                }
            }
        }
    }

    fun onVerifyClicked() {
        viewModelScope.launch {
            _verificationState.value = VerificationState.Loading
            val tempProbe = ProbeConfig(
                name = name.value,
                ipAddress = ipAddress.value,
                username = username.value,
                password = password.value,
                isHttps = isHttps.value,
                testInterface = "", isOnline = false, modelName = null, tdrSupported = false
            )
            when (val result = repository.checkProbeConnection(tempProbe)) {
                is ProbeCheckResult.Success -> {
                    _isOnline.value = true
                    _modelName.value = result.boardName
                    _tdrSupported.value = Compatibility.isTdrSupported(result.boardName)
                    _availableInterfaces.value = result.interfaces
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
                probeId = if (isEditing) probeId else 0,
                name = name.value,
                ipAddress = ipAddress.value,
                username = username.value,
                password = password.value,
                isHttps = isHttps.value,
                testInterface = testInterface.value,
                isOnline = _isOnline.value,
                modelName = _modelName.value,
                tdrSupported = _tdrSupported.value
            )
            probeConfigDao.insert(probeToSave)
            _isSaved.value = true
        }
    }
}

sealed class VerificationState {
    object Idle : VerificationState()
    object Loading : VerificationState()
    data class Success(val boardName: String?, val interfaces: List<String>) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

package com.app.miklink.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.BackupRepository
import com.app.miklink.core.data.io.DocumentDestination
import com.app.miklink.core.data.io.DocumentReader
import com.app.miklink.core.data.io.DocumentWriter
import com.app.miklink.core.domain.usecase.backup.ImportBackupUseCase
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import com.app.miklink.core.domain.usecase.preferences.ObserveIdNumberingStrategyUseCase
import com.app.miklink.core.domain.usecase.preferences.SetIdNumberingStrategyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val importBackupUseCase: ImportBackupUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val observeIdNumberingStrategyUseCase: ObserveIdNumberingStrategyUseCase,
    private val setIdNumberingStrategyUseCase: SetIdNumberingStrategyUseCase,
    private val documentWriter: DocumentWriter,
    private val documentReader: DocumentReader
) : ViewModel() {

    private val _backupStatus = MutableStateFlow("")
    val backupStatus = _backupStatus.asStateFlow()

    val idNumberingStrategy = observeIdNumberingStrategyUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = IdNumberingStrategy.CONTINUOUS_INCREMENT
        )

    fun updateIdNumberingStrategy(strategy: IdNumberingStrategy) {
        viewModelScope.launch {
            setIdNumberingStrategyUseCase(strategy)
        }
    }

    val pdfIncludeEmptyTests = userPreferencesRepository.pdfIncludeEmptyTests
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val pdfSelectedColumns = userPreferencesRepository.pdfSelectedColumns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.app.miklink.core.data.pdf.ExportColumn.values().map { it.name }.toSet()
        )

    fun updatePdfIncludeEmptyTests(include: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPdfIncludeEmptyTests(include)
        }
    }

    fun updatePdfSelectedColumns(columns: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setPdfSelectedColumns(columns)
        }
    }

    val pdfReportTitle = userPreferencesRepository.pdfReportTitle
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val pdfHideEmptyColumns = userPreferencesRepository.pdfHideEmptyColumns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updatePdfReportTitle(title: String) {
        viewModelScope.launch {
            userPreferencesRepository.setPdfReportTitle(title)
        }
    }

    fun updatePdfHideEmptyColumns(hide: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPdfHideEmptyColumns(hide)
        }
    }

    val dashboardGlowIntensity = userPreferencesRepository.dashboardGlowIntensity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        )

    fun updateDashboardGlowIntensity(intensity: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setDashboardGlowIntensity(intensity)
        }
    }

    val probePollingInterval = userPreferencesRepository.probePollingInterval
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5000L
        )

    fun updateProbePollingInterval(interval: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setProbePollingInterval(interval)
        }
    }

    suspend fun exportConfig(): String {
        return backupRepository.exportConfigToJson()
    }

    fun importConfig(uri: Uri) {
        viewModelScope.launch { importConfigSuspend(uri) }
    }

    // Made suspend for better testability â€” tests can call this directly instead of relying on viewModelScope
    suspend fun importConfigSuspend(uri: Uri) {
        val readResult = documentReader.readText(DocumentDestination(uriString = uri.toString()))
        readResult.onFailure { error ->
            _backupStatus.value = "Error restoring configuration: ${error.message}"
            return
        }

        val json = readResult.getOrNull()
        if (json.isNullOrBlank()) {
            _backupStatus.value = "Error: Selected file is empty."
            return
        }

        val result = importBackupUseCase.execute(json)
        _backupStatus.value = if (result.isSuccess) {
            "Configuration restored successfully."
        } else {
            "Error restoring configuration: ${result.exceptionOrNull()?.message}"
        }
    }

    fun saveExportToUri(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = exportConfig()
                val writeResult = documentWriter.writeBytes(
                    DocumentDestination(uriString = uri.toString()),
                    json.toByteArray(),
                    "application/json"
                )
                _backupStatus.value = if (writeResult.isSuccess) {
                    "Backup saved successfully."
                } else {
                    "Error saving backup: ${writeResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _backupStatus.value = "Error saving backup: ${e.message}"
            }
        }
    }
}

package com.app.miklink.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.BackupRepository
import com.app.miklink.data.io.FileReader
import com.app.miklink.domain.usecase.backup.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.app.miklink.data.repository.IdNumberingStrategy
import com.app.miklink.data.repository.ThemeConfig
import com.app.miklink.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val importBackupUseCase: ImportBackupUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val fileReader: FileReader,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _backupStatus = MutableStateFlow("")
    val backupStatus = _backupStatus.asStateFlow()

    val themeConfig = userPreferencesRepository.themeConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeConfig.FOLLOW_SYSTEM
        )

    val customPalette = userPreferencesRepository.customPalette
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesRepository.CustomPalette()
        )

    val idNumberingStrategy = userPreferencesRepository.idNumberingStrategy
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = IdNumberingStrategy.CONTINUOUS_INCREMENT
        )

    fun updateTheme(config: ThemeConfig) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(config)
        }
    }

    fun updateIdNumberingStrategy(strategy: IdNumberingStrategy) {
        viewModelScope.launch {
            userPreferencesRepository.setIdNumberingStrategy(strategy)
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
            initialValue = com.app.miklink.data.pdf.ExportColumn.values().map { it.name }.toSet()
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
            initialValue = "Collaudo Cablaggio di Rete"
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

    fun updateCustomPalette(primary: Int?, secondary: Int?, background: Int?, content: Int? = null) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomPalette(primary, secondary, background, content)
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

    // Made suspend for better testability — tests can call this directly instead of relying on viewModelScope
    suspend fun importConfigSuspend(uri: Uri) {
        try {
            val json = fileReader.read(uri)
            if (!json.isNullOrBlank()) {
                val result = importBackupUseCase.execute(json)
                if (result.isSuccess) {
                    _backupStatus.value = "Configuration restored successfully."
                } else {
                    _backupStatus.value = "Error restoring configuration: ${result.exceptionOrNull()?.message}"
                }
            } else {
                _backupStatus.value = "Error: Selected file is empty."
            }
        } catch (e: Exception) {
            _backupStatus.value = "Error restoring configuration: ${e.message}"
        }
    }
}

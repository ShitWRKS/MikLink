package com.app.miklink.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.repository.BackupRepository
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
    private val userPreferencesRepository: UserPreferencesRepository,
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

    fun updateCustomPalette(primary: Int?, secondary: Int?, background: Int?, content: Int? = null) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomPalette(primary, secondary, background, content)
        }
    }

    suspend fun exportConfig(): String {
        return backupRepository.exportConfigToJson()
    }

    fun importConfig(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (!json.isNullOrBlank()) {
                    backupRepository.importConfigFromJson(json)
                    _backupStatus.value = "Configuration restored successfully."
                } else {
                    _backupStatus.value = "Error: Selected file is empty."
                }
            } catch (e: Exception) {
                _backupStatus.value = "Error restoring configuration: ${e.message}"
            }
        }
    }
}

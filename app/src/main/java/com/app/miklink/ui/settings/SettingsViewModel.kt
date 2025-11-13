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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _backupStatus = MutableStateFlow("")
    val backupStatus = _backupStatus.asStateFlow()

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

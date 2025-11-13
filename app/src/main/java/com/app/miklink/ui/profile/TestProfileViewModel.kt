package com.app.miklink.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.TestProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestProfileViewModel @Inject constructor(
    private val testProfileDao: TestProfileDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // For the list screen
    val profiles: StateFlow<List<TestProfile>> = testProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- For the edit screen ---
    private val profileId: Long = savedStateHandle.get<Long>("profileId") ?: -1L
    val isEditing = profileId != -1L

    // Form fields
    val profileName = MutableStateFlow("")
    val profileDescription = MutableStateFlow("")
    val runTdr = MutableStateFlow(false)
    val runLinkStatus = MutableStateFlow(true)
    val runLldp = MutableStateFlow(false)
    val runPing = MutableStateFlow(false)
    val runTraceroute = MutableStateFlow(false)
    val pingTarget1 = MutableStateFlow("")
    val pingTarget2 = MutableStateFlow("")
    val pingTarget3 = MutableStateFlow("")

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                testProfileDao.getProfileById(profileId).firstOrNull()?.let { profile ->
                    profileName.value = profile.profileName
                    profileDescription.value = profile.profileDescription ?: ""
                    runTdr.value = profile.runTdr
                    runLinkStatus.value = profile.runLinkStatus
                    runLldp.value = profile.runLldp
                    runPing.value = profile.runPing
                    runTraceroute.value = profile.runTraceroute
                    pingTarget1.value = profile.pingTarget1 ?: ""
                    pingTarget2.value = profile.pingTarget2 ?: ""
                    pingTarget3.value = profile.pingTarget3 ?: ""
                }
            }
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val profile = TestProfile(
                profileId = if (isEditing) profileId else 0,
                profileName = profileName.value,
                profileDescription = profileDescription.value,
                runTdr = runTdr.value,
                runLinkStatus = runLinkStatus.value,
                runLldp = runLldp.value,
                runPing = runPing.value,
                runTraceroute = runTraceroute.value,
                pingTarget1 = pingTarget1.value.takeIf { it.isNotBlank() },
                pingTarget2 = pingTarget2.value.takeIf { it.isNotBlank() },
                pingTarget3 = pingTarget3.value.takeIf { it.isNotBlank() }
            )
            testProfileDao.insert(profile)
            _isSaved.value = true
        }
    }

    fun deleteProfile(profile: TestProfile) {
        viewModelScope.launch {
            testProfileDao.delete(profile)
        }
    }
}

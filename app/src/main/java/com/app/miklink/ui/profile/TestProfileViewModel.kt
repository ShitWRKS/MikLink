package com.app.miklink.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.local.room.v1.dao.TestProfileDao
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import com.app.miklink.ui.common.BaseEditViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestProfileViewModel @Inject constructor(
    private val testProfileDao: TestProfileDao,
    private val savedStateHandle: SavedStateHandle
) : BaseEditViewModel(savedStateHandle, "profileId") {

    // For the list screen
    val profiles: StateFlow<List<TestProfile>> = testProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- For the edit screen ---
    // entityId and isEditing provided by BaseEditViewModel

    // Form fields
    val profileName = MutableStateFlow("")
    val profileDescription = MutableStateFlow("")
    val runTdr = MutableStateFlow(false)
    val runLinkStatus = MutableStateFlow(true)
    val runLldp = MutableStateFlow(false)
    val runPing = MutableStateFlow(false)
    val pingTarget1 = MutableStateFlow("")
    val pingTarget2 = MutableStateFlow("")
    val pingTarget3 = MutableStateFlow("")
    val pingCount = MutableStateFlow("4") // count ping (1-20)
    val runSpeedTest = MutableStateFlow(false)

    override suspend fun loadEntity(id: Long) {
        testProfileDao.getProfileById(id).firstOrNull()?.let { profile ->
            profileName.value = profile.profileName
            profileDescription.value = profile.profileDescription ?: ""
            runTdr.value = profile.runTdr
            runLinkStatus.value = profile.runLinkStatus
            runLldp.value = profile.runLldp
            runPing.value = profile.runPing
            pingTarget1.value = profile.pingTarget1 ?: ""
            pingTarget2.value = profile.pingTarget2 ?: ""
            pingTarget3.value = profile.pingTarget3 ?: ""
            pingCount.value = profile.pingCount.toString()
            runSpeedTest.value = profile.runSpeedTest
        }
    }

    init { loadIfEditing() }

    fun saveProfile() {
        viewModelScope.launch {
            val profile = TestProfile(
                profileId = if (isEditing) entityId else 0,
                profileName = profileName.value,
                profileDescription = profileDescription.value,
                runTdr = runTdr.value,
                runLinkStatus = runLinkStatus.value,
                runLldp = runLldp.value,
                runPing = runPing.value,
                pingTarget1 = pingTarget1.value.takeIf { it.isNotBlank() },
                pingTarget2 = pingTarget2.value.takeIf { it.isNotBlank() },
                pingTarget3 = pingTarget3.value.takeIf { it.isNotBlank() },
                pingCount = pingCount.value.toIntOrNull()?.coerceIn(1, 20) ?: 4, // validation
                runSpeedTest = runSpeedTest.value
            )
            testProfileDao.insert(profile)
            markSaved()
        }
    }

    fun deleteProfile(profile: TestProfile) {
        viewModelScope.launch { testProfileDao.delete(profile) }
    }

    fun fillLastAvailableTarget(value: String) {
        when {
            pingTarget1.value.isBlank() -> pingTarget1.value = value
            pingTarget2.value.isBlank() -> pingTarget2.value = value
            pingTarget3.value.isBlank() -> pingTarget3.value = value
        }
    }

    val availableSlots: StateFlow<Int> = combine(pingTarget1, pingTarget2, pingTarget3) { t1, t2, t3 ->
        listOf(t1, t2, t3).count { it.isBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    // Validation helper for UI/tests (not part of BaseEditViewModel contract)
    fun isValidForSave(): Boolean = profileName.value.isNotBlank()
}


package com.app.miklink.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.TestProfile
import com.app.miklink.ui.common.BaseEditViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestProfileViewModel @Inject constructor(
    private val testProfileDao: TestProfileDao,
    savedStateHandle: SavedStateHandle
) : BaseEditViewModel<TestProfile>(savedStateHandle, "profileId") {

    // For the list screen
    val profiles: StateFlow<List<TestProfile>> = testProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    init {
        loadInitialData()
    }

    override suspend fun loadEntityById(id: Long): TestProfile? {
        return testProfileDao.getProfileById(id).firstOrNull()
    }

    override fun populateFormFields(entity: TestProfile) {
        profileName.value = entity.profileName
        profileDescription.value = entity.profileDescription ?: ""
        runTdr.value = entity.runTdr
        runLinkStatus.value = entity.runLinkStatus
        runLldp.value = entity.runLldp
        runPing.value = entity.runPing
        pingTarget1.value = entity.pingTarget1 ?: ""
        pingTarget2.value = entity.pingTarget2 ?: ""
        pingTarget3.value = entity.pingTarget3 ?: ""
        pingCount.value = entity.pingCount.toString()
        runSpeedTest.value = entity.runSpeedTest
    }

    override fun buildEntityFromForm(): TestProfile {
        return TestProfile(
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
    }

    override suspend fun persistEntity(entity: TestProfile) {
        testProfileDao.insert(entity)
    }

    /**
     * Auto-fills the last available ping target slot with the provided value.
     * Used by quick-fill toggle buttons (DHCP Gateway, Google DNS, Cloudflare DNS).
     */
    fun fillLastAvailableTarget(value: String) {
        when {
            pingTarget1.value.isBlank() -> pingTarget1.value = value
            pingTarget2.value.isBlank() -> pingTarget2.value = value
            pingTarget3.value.isBlank() -> pingTarget3.value = value
            // All slots filled, do nothing or replace last one
        }
    }

    /**
     * Returns the number of available (empty) ping target slots.
     */
    val availableSlots: StateFlow<Int> = combine(pingTarget1, pingTarget2, pingTarget3) { t1, t2, t3 ->
        listOf(t1, t2, t3).count { it.isBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    fun deleteProfile(profile: TestProfile) {
        viewModelScope.launch {
            testProfileDao.delete(profile)
        }
    }

    /**
     * Public API to save a profile from the UI/tests. This delegates to the
     * BaseEditViewModel.save() flow so the persistence and lifecycle behaviour
     * (isSaved flow, validation hook) are consistently applied.
     */
    fun saveProfile() {
        save()
    }

    /**
     * Require a profile name to be present before saving. This is a small,
     * intentional validation rule that prevents saving empty/unnamed profiles
     * and keeps data quality high.
     */
    override fun validateBeforeSave(): Boolean = profileName.value.isNotBlank()
}


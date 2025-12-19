/*
 * Purpose: Manage test profile list/edit state and persist profiles through the guarded save use case.
 * Inputs: SavedStateHandle (profileId arg), TestProfileRepository for reads, and SaveTestProfileUseCase for writes.
 * Outputs: Profiles stream for the list screen plus save/delete operations without PK constraint crashes.
 * Notes: Never calls insert on existing profiles; validates ping count bounds before saving.
 */
package com.app.miklink.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestThresholds
import com.app.miklink.core.domain.model.PingThresholds
import com.app.miklink.core.domain.model.SpeedThresholds
import com.app.miklink.core.domain.model.GatewayUnresolvedPolicy
import com.app.miklink.core.domain.usecase.testprofile.SaveTestProfileUseCase
import com.app.miklink.ui.common.BaseEditViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestProfileViewModel @Inject constructor(
    private val testProfileRepository: TestProfileRepository,
    private val saveTestProfileUseCase: SaveTestProfileUseCase,
    private val savedStateHandle: SavedStateHandle
) : BaseEditViewModel(savedStateHandle, "profileId") {

    // For the list screen
    val profiles: StateFlow<List<TestProfile>> = testProfileRepository.observeAllProfiles()
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
    private val defaultThresholds = TestThresholds.defaults()

    // Threshold fields (stored as string for UI inputs)
    val linkMinRate = MutableStateFlow(defaultThresholds.linkMinRate ?: "")

    val pingLocalMaxLoss = MutableStateFlow(defaultThresholds.pingLocal.maxLossPercent.toString())
    val pingLocalMaxAvgRtt = MutableStateFlow(defaultThresholds.pingLocal.maxAvgRttMs.toString())
    val pingLocalMaxRtt = MutableStateFlow(defaultThresholds.pingLocal.maxRttMs.toString())

    val pingExternalMaxLoss = MutableStateFlow(defaultThresholds.pingExternal.maxLossPercent.toString())
    val pingExternalMaxAvgRtt = MutableStateFlow(defaultThresholds.pingExternal.maxAvgRttMs.toString())
    val pingExternalMaxRtt = MutableStateFlow(defaultThresholds.pingExternal.maxRttMs.toString())

    val gatewayPolicyFail = MutableStateFlow(defaultThresholds.gatewayPolicy == GatewayUnresolvedPolicy.FAIL)

    val speedMaxPing = MutableStateFlow(defaultThresholds.speed.maxPingMs.toString())
    val speedMaxJitter = MutableStateFlow(defaultThresholds.speed.maxJitterMs.toString())
    val speedMaxLoss = MutableStateFlow(defaultThresholds.speed.maxLossPercent.toString())
    val speedMinDownload = MutableStateFlow(defaultThresholds.speed.minDownloadMbps.toString())
    val speedMinUpload = MutableStateFlow(defaultThresholds.speed.minUploadMbps.toString())

    override suspend fun loadEntity(id: Long) {
        testProfileRepository.getProfile(id)?.let { profile ->
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
            val thresholds = profile.thresholds
            linkMinRate.value = thresholds.linkMinRate ?: ""
            pingLocalMaxLoss.value = thresholds.pingLocal.maxLossPercent.toString()
            pingLocalMaxAvgRtt.value = thresholds.pingLocal.maxAvgRttMs.toString()
            pingLocalMaxRtt.value = thresholds.pingLocal.maxRttMs.toString()
            pingExternalMaxLoss.value = thresholds.pingExternal.maxLossPercent.toString()
            pingExternalMaxAvgRtt.value = thresholds.pingExternal.maxAvgRttMs.toString()
            pingExternalMaxRtt.value = thresholds.pingExternal.maxRttMs.toString()
            gatewayPolicyFail.value = thresholds.gatewayPolicy == GatewayUnresolvedPolicy.FAIL
            speedMaxPing.value = thresholds.speed.maxPingMs.toString()
            speedMaxJitter.value = thresholds.speed.maxJitterMs.toString()
            speedMaxLoss.value = thresholds.speed.maxLossPercent.toString()
            speedMinDownload.value = thresholds.speed.minDownloadMbps.toString()
            speedMinUpload.value = thresholds.speed.minUploadMbps.toString()
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
                runSpeedTest = runSpeedTest.value,
                thresholds = buildThresholds()
            )
            saveTestProfileUseCase(profile)
            markSaved()
        }
    }

    fun deleteProfile(profile: TestProfile) {
        viewModelScope.launch { testProfileRepository.deleteProfile(profile) }
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

    private fun buildThresholds(): TestThresholds {
        fun String.toDoubleOrDefault(default: Double) = this.toDoubleOrNull() ?: default
        val local = PingThresholds(
            maxLossPercent = pingLocalMaxLoss.value.toDoubleOrDefault(defaultThresholds.pingLocal.maxLossPercent),
            maxAvgRttMs = pingLocalMaxAvgRtt.value.toDoubleOrDefault(defaultThresholds.pingLocal.maxAvgRttMs),
            maxRttMs = pingLocalMaxRtt.value.toDoubleOrDefault(defaultThresholds.pingLocal.maxRttMs)
        )
        val external = PingThresholds(
            maxLossPercent = pingExternalMaxLoss.value.toDoubleOrDefault(defaultThresholds.pingExternal.maxLossPercent),
            maxAvgRttMs = pingExternalMaxAvgRtt.value.toDoubleOrDefault(defaultThresholds.pingExternal.maxAvgRttMs),
            maxRttMs = pingExternalMaxRtt.value.toDoubleOrDefault(defaultThresholds.pingExternal.maxRttMs)
        )
        val speed = SpeedThresholds(
            maxPingMs = speedMaxPing.value.toDoubleOrDefault(defaultThresholds.speed.maxPingMs),
            maxJitterMs = speedMaxJitter.value.toDoubleOrDefault(defaultThresholds.speed.maxJitterMs),
            maxLossPercent = speedMaxLoss.value.toDoubleOrDefault(defaultThresholds.speed.maxLossPercent),
            minDownloadMbps = speedMinDownload.value.toDoubleOrDefault(defaultThresholds.speed.minDownloadMbps),
            minUploadMbps = speedMinUpload.value.toDoubleOrDefault(defaultThresholds.speed.minUploadMbps)
        )
        return TestThresholds(
            linkMinRate = linkMinRate.value.ifBlank { defaultThresholds.linkMinRate },
            tdrFailStatuses = defaultThresholds.tdrFailStatuses,
            pingLocal = local,
            pingExternal = external,
            gatewayPolicy = if (gatewayPolicyFail.value) GatewayUnresolvedPolicy.FAIL else GatewayUnresolvedPolicy.SKIP,
            speed = speed
        )
    }
}

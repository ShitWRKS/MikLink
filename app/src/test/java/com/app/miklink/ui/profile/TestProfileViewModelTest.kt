/*
 * Purpose: Validate TestProfileViewModel save guards and profile form validation rules.
 * Inputs: Fake repository/use case and controlled form field state transitions.
 * Outputs: Assertions on isValidForSave and saveProfile behavior on invalid input.
 */
package com.app.miklink.ui.profile

import androidx.lifecycle.SavedStateHandle
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.usecase.testprofile.SaveTestProfileUseCase
import com.app.miklink.testsupport.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.app.miklink.core.domain.model.TestThresholds

@OptIn(ExperimentalCoroutinesApi::class)
class TestProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `isValidForSave false with blank name`() {
        val viewModel = createViewModel()
        viewModel.profileName.value = ""
        viewModel.runLinkStatus.value = true

        assertFalse(viewModel.isValidForSave())
    }

    @Test
    fun `isValidForSave false with all tests disabled`() {
        val viewModel = createViewModel()
        viewModel.profileName.value = "Profile"
        viewModel.runLinkStatus.value = false
        viewModel.runTdr.value = false
        viewModel.runLldp.value = false
        viewModel.runPing.value = false
        viewModel.runSpeedTest.value = false

        assertFalse(viewModel.isValidForSave())
    }

    @Test
    fun `isValidForSave true with ping enabled and empty targets`() {
        val viewModel = createViewModel()
        viewModel.profileName.value = "Profile"
        viewModel.runLinkStatus.value = false
        viewModel.runPing.value = true
        viewModel.pingTarget1.value = ""
        viewModel.pingTarget2.value = ""
        viewModel.pingTarget3.value = ""
        viewModel.pingCount.value = "4"

        assertTrue(viewModel.isValidForSave())
    }

    @Test
    fun `isValidForSave false with ping enabled and invalid target`() {
        val viewModel = createViewModel()
        viewModel.profileName.value = "Profile"
        viewModel.runLinkStatus.value = false
        viewModel.runPing.value = true
        viewModel.pingTarget1.value = "http://invalid-target"
        viewModel.pingCount.value = "4"

        assertFalse(viewModel.isValidForSave())
    }

    @Test
    fun `saveProfile does not call use case when form is invalid`() = runTest {
        val fakeUseCase = FakeSaveTestProfileUseCase()
        val viewModel = createViewModel(saveUseCase = fakeUseCase)
        viewModel.profileName.value = ""
        viewModel.runLinkStatus.value = true

        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(0, fakeUseCase.calls)
        assertFalse(viewModel.isSaved.value)
    }

    @Test
    fun `blank threshold is valid and saves its default`() = runTest {
        val fakeUseCase = FakeSaveTestProfileUseCase()
        val viewModel = createValidViewModel(fakeUseCase)
        viewModel.pingLocalMaxLoss.value = ""

        assertTrue(viewModel.isValidForSave())
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(TestThresholds.defaults().pingLocal.maxLossPercent, fakeUseCase.lastProfile?.thresholds?.pingLocal?.maxLossPercent)
    }

    @Test
    fun `non numeric negative and non finite thresholds are invalid`() {
        val viewModel = createValidViewModel()

        listOf("not-a-number", "-1", "NaN", "Infinity").forEach { invalid ->
            viewModel.speedMaxPing.value = invalid
            assertFalse("Expected $invalid to be invalid", viewModel.isValidForSave())
        }
    }

    @Test
    fun `loss accepts boundaries and rejects values outside percentage range`() {
        val viewModel = createValidViewModel()

        listOf("0", "100", "12.5").forEach { valid ->
            viewModel.speedMaxLoss.value = valid
            assertTrue("Expected $valid to be valid", viewModel.isValidForSave())
        }
        listOf("-0.1", "100.1").forEach { invalid ->
            viewModel.speedMaxLoss.value = invalid
            assertFalse("Expected $invalid to be invalid", viewModel.isValidForSave())
        }
    }

    @Test
    fun `speed throughput accepts 100G boundary and rejects larger values`() {
        val viewModel = createValidViewModel()

        listOf("0", "99999.99", "100000").forEach { valid ->
            viewModel.speedMinDownload.value = valid
            viewModel.speedMinUpload.value = valid
            assertTrue("Expected $valid Mbps to be valid", viewModel.isValidForSave())
        }
        listOf("100000.01", "100001").forEach { invalid ->
            viewModel.speedMinDownload.value = invalid
            assertFalse("Expected $invalid Mbps to be invalid", viewModel.isValidForSave())
        }
    }

    @Test
    fun `custom link rate must match the policy format`() {
        val viewModel = createValidViewModel()

        viewModel.linkMinRate.value = "fast"
        assertFalse(viewModel.isValidForSave())

        viewModel.linkMinRate.value = "2.5G"
        assertTrue(viewModel.isValidForSave())
    }

    @Test
    fun `preview helpers use owned defaults for blank fields without mutating input`() {
        val viewModel = createValidViewModel()
        viewModel.pingLocalMaxAvgRtt.value = ""
        viewModel.speedMinDownload.value = ""

        assertEquals(
            TestThresholds.defaults().pingLocal.maxAvgRttMs,
            viewModel.effectivePingLocalMaxAvgRttForPreview()
        )
        assertEquals(
            TestThresholds.defaults().speed.minDownloadMbps,
            viewModel.effectiveSpeedMinDownloadForPreview()
        )
        assertEquals("", viewModel.pingLocalMaxAvgRtt.value)
        assertEquals("", viewModel.speedMinDownload.value)
    }

    @Test
    fun `preview helpers omit invalid values instead of inventing fallbacks`() {
        val viewModel = createValidViewModel()
        viewModel.pingExternalMaxRtt.value = "invalid"
        viewModel.speedMaxLoss.value = "101"

        assertEquals(null, viewModel.effectivePingExternalMaxRttForPreview())
        assertEquals(null, viewModel.effectiveSpeedMaxLossForPreview())
    }

    private fun createValidViewModel(saveUseCase: SaveTestProfileUseCase = FakeSaveTestProfileUseCase()): TestProfileViewModel =
        createViewModel(saveUseCase = saveUseCase).also {
            it.profileName.value = "Profile"
            it.runLinkStatus.value = true
        }

    private fun createViewModel(
        repository: TestProfileRepository = FakeTestProfileRepository(),
        saveUseCase: SaveTestProfileUseCase = FakeSaveTestProfileUseCase()
    ): TestProfileViewModel {
        return TestProfileViewModel(
            testProfileRepository = repository,
            saveTestProfileUseCase = saveUseCase,
            savedStateHandle = SavedStateHandle()
        )
    }

    private class FakeTestProfileRepository : TestProfileRepository {
        override fun observeAllProfiles(): Flow<List<TestProfile>> = emptyFlow()

        override suspend fun getProfile(id: Long): TestProfile? = null

        override suspend fun insertProfile(profile: TestProfile): Long = 1L

        override suspend fun updateProfile(profile: TestProfile) = Unit

        override suspend fun deleteProfile(profile: TestProfile) = Unit
    }

    private class FakeSaveTestProfileUseCase : SaveTestProfileUseCase {
        var calls = 0
        var lastProfile: TestProfile? = null

        override suspend fun invoke(profile: TestProfile): Long {
            calls += 1
            lastProfile = profile
            return 1L
        }
    }
}

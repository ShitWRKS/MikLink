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

        override suspend fun invoke(profile: TestProfile): Long {
            calls += 1
            return 1L
        }
    }
}

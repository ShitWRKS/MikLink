/*
 * Purpose: Ensure SaveTestProfileUseCase performs insert for new profiles and update for existing ones to avoid UNIQUE conflicts.
 * Inputs: Fake TestProfileRepository tracking insert/update invocations.
 * Outputs: Assertions on the chosen persistence path and returned identifiers.
 * Notes: Supports GR3 by preventing insert calls on edits at the use-case boundary.
 */
package com.app.miklink.core.domain.usecase.testprofile

import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveTestProfileUseCaseTest {
    private val fakeRepository = FakeTestProfileRepository()
    private val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

    @Test
    fun `inserts new profile when id is zero`() = runBlocking {
        val id = useCase(profile(profileId = 0))

        assertTrue(fakeRepository.insertCalled)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(99L, id)
    }

    @Test
    fun `updates existing profile when id is set`() = runBlocking {
        val id = useCase(profile(profileId = 5))

        assertEquals(5, fakeRepository.updateCalls)
        assertTrue(fakeRepository.insertCalled.not())
        assertEquals(5L, id)
    }

    private class FakeTestProfileRepository : TestProfileRepository {
        var insertCalled = false
        var updateCalls = 0

        override fun observeAllProfiles(): Flow<List<TestProfile>> = emptyFlow()

        override suspend fun getProfile(id: Long): TestProfile? = null

        override suspend fun insertProfile(profile: TestProfile): Long {
            insertCalled = true
            return 99L
        }

        override suspend fun updateProfile(profile: TestProfile) {
            updateCalls = profile.profileId.toInt()
        }

        override suspend fun deleteProfile(profile: TestProfile) = Unit
    }

    private fun profile(profileId: Long) = TestProfile(
        profileId = profileId,
        profileName = "Full Test",
        profileDescription = "",
        runTdr = true,
        runLinkStatus = true,
        runLldp = true,
        runPing = true,
        pingTarget1 = null,
        pingTarget2 = null,
        pingTarget3 = null,
        pingCount = 4,
        runSpeedTest = false,
        thresholds = com.app.miklink.core.domain.model.TestThresholds.defaults()
    )
}

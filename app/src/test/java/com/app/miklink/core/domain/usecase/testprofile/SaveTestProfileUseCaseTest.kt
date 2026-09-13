/*
 * Purpose: Validate SaveTestProfileUseCase persistence path and domain guards for test profile saves.
 * Inputs: Fake TestProfileRepository tracking insert/update invocations and synthetic TestProfile instances.
 * Outputs: Assertions on successful saves and IllegalArgumentException for invalid profiles.
 */
package com.app.miklink.core.domain.usecase.testprofile

import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SaveTestProfileUseCaseTest {
    @Test
    fun `valid profile with link status enabled saves`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        val id = useCase(profile(runLinkStatus = true))

        assertEquals(1, fakeRepository.insertCalls)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(99L, id)
    }

    @Test
    fun `blank profile name fails and does not call repository`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        assertValidationFailsWithoutSaving(
            useCase = useCase,
            repository = fakeRepository,
            profile = profile(profileName = "   ")
        )
    }

    @Test
    fun `all tests disabled fails and does not call repository`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        assertValidationFailsWithoutSaving(
            useCase = useCase,
            repository = fakeRepository,
            profile = profile(
                runLinkStatus = false,
                runTdr = false,
                runLldp = false,
                runPing = false,
                runSpeedTest = false
            )
        )
    }

    @Test
    fun `runPing false with invalid targets saves`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        val id = useCase(
            profile(
                runLinkStatus = true,
                runPing = false,
                pingTarget1 = "https://invalid.example"
            )
        )

        assertEquals(1, fakeRepository.insertCalls)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(99L, id)
    }

    @Test
    fun `runPing true with all targets empty saves`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        val id = useCase(
            profile(
                runLinkStatus = false,
                runPing = true,
                pingTarget1 = null,
                pingTarget2 = null,
                pingTarget3 = null
            )
        )

        assertEquals(1, fakeRepository.insertCalls)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(99L, id)
    }

    @Test
    fun `runPing true with invalid target fails and does not call repository`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        assertValidationFailsWithoutSaving(
            useCase = useCase,
            repository = fakeRepository,
            profile = profile(
                runLinkStatus = false,
                runPing = true,
                pingTarget1 = "http://invalid-target"
            )
        )
    }

    @Test
    fun `runPing true with pingCount 0 fails`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        assertValidationFailsWithoutSaving(
            useCase = useCase,
            repository = fakeRepository,
            profile = profile(
                runLinkStatus = false,
                runPing = true,
                pingCount = 0
            )
        )
    }

    @Test
    fun `runPing true with pingCount 21 fails`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        assertValidationFailsWithoutSaving(
            useCase = useCase,
            repository = fakeRepository,
            profile = profile(
                runLinkStatus = false,
                runPing = true,
                pingCount = 21
            )
        )
    }

    @Test
    fun `runPing true with valid pingCount saves`() = runBlocking {
        val fakeRepository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(fakeRepository)

        val id = useCase(
            profile(
                runLinkStatus = false,
                runPing = true,
                pingTarget1 = "8.8.8.8",
                pingCount = 7
            )
        )

        assertEquals(1, fakeRepository.insertCalls)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(99L, id)
    }

    @Test
    fun `percentage thresholds outside boundaries do not reach repository`() = runBlocking {
        listOf(-0.1, 100.1).forEach { invalid ->
            val repository = FakeTestProfileRepository()
            val useCase = SaveTestProfileUseCaseImpl(repository)
            val thresholds = TestThresholds.defaults().copy(
                pingLocal = TestThresholds.defaults().pingLocal.copy(maxLossPercent = invalid)
            )

            assertValidationFailsWithoutSaving(useCase, repository, profile(thresholds = thresholds))
        }
    }

    @Test
    fun `negative and non finite numeric thresholds do not reach repository`() = runBlocking {
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { invalid ->
            val repository = FakeTestProfileRepository()
            val useCase = SaveTestProfileUseCaseImpl(repository)
            val thresholds = TestThresholds.defaults().copy(
                speed = TestThresholds.defaults().speed.copy(minDownloadMbps = invalid)
            )

            assertValidationFailsWithoutSaving(useCase, repository, profile(thresholds = thresholds))
        }
    }

    @Test
    fun `invalid link rate does not reach repository`() = runBlocking {
        val repository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(repository)

        assertValidationFailsWithoutSaving(
            useCase,
            repository,
            profile(thresholds = TestThresholds.defaults().copy(linkMinRate = "not-a-rate"))
        )
    }

    @Test
    fun `percentage boundaries and valid custom link rate save`() = runBlocking {
        val repository = FakeTestProfileRepository()
        val useCase = SaveTestProfileUseCaseImpl(repository)
        val defaults = TestThresholds.defaults()
        val thresholds = defaults.copy(
            linkMinRate = "2.5G",
            pingLocal = defaults.pingLocal.copy(maxLossPercent = 0.0),
            pingExternal = defaults.pingExternal.copy(maxLossPercent = 100.0)
        )

        useCase(profile(thresholds = thresholds))

        assertEquals(1, repository.insertCalls)
    }

    private class FakeTestProfileRepository : TestProfileRepository {
        var insertCalls = 0
        var updateCalls = 0

        override fun observeAllProfiles(): Flow<List<TestProfile>> = emptyFlow()

        override suspend fun getProfile(id: Long): TestProfile? = null

        override suspend fun insertProfile(profile: TestProfile): Long {
            insertCalls += 1
            return 99L
        }

        override suspend fun updateProfile(profile: TestProfile) {
            updateCalls += 1
        }

        override suspend fun deleteProfile(profile: TestProfile) = Unit
    }

    private suspend fun assertValidationFailsWithoutSaving(
        useCase: SaveTestProfileUseCase,
        repository: FakeTestProfileRepository,
        profile: TestProfile
    ) {
        try {
            useCase(profile)
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertFalse(repository.insertCalls > 0)
        assertEquals(0, repository.updateCalls)
    }

    private fun profile(
        profileId: Long = 0,
        profileName: String = "Full Test",
        runTdr: Boolean = false,
        runLinkStatus: Boolean = true,
        runLldp: Boolean = false,
        runPing: Boolean = false,
        pingTarget1: String? = null,
        pingTarget2: String? = null,
        pingTarget3: String? = null,
        pingCount: Int = 4,
        runSpeedTest: Boolean = false,
        thresholds: TestThresholds = TestThresholds.defaults()
    ) = TestProfile(
        profileId = profileId,
        profileName = profileName,
        profileDescription = "",
        runTdr = runTdr,
        runLinkStatus = runLinkStatus,
        runLldp = runLldp,
        runPing = runPing,
        pingTarget1 = pingTarget1,
        pingTarget2 = pingTarget2,
        pingTarget3 = pingTarget3,
        pingCount = pingCount,
        runSpeedTest = runSpeedTest,
        thresholds = thresholds
    )
}

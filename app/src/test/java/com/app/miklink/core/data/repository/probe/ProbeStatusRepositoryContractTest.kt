/*
 * Purpose: Contract tests for probe status repository to validate online detection via MikroTik REST.
 * Inputs: Mocked service provider responses and probe configuration.
 * Outputs: Assertions that online/offline status is determined correctly when board-name is present or errors occur.
 * Notes: Guards against regressions in board-name filtering and polling logic.
 */
package com.app.miklink.core.data.repository.probe

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.dto.ProplistRequest
import com.app.miklink.data.remote.mikrotik.dto.SystemResource
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.data.repositoryimpl.mikrotik.ProbeStatusRepositoryImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

/**
 * Contract test per ProbeStatusRepository.
 * Verifica il comportamento di monitoraggio stato online/offline delle sonde.
 */
class ProbeStatusRepositoryContractTest {

    private val mockProbeRepository = mockk<ProbeRepository>()
    private val mockServiceProvider = mockk<MikroTikServiceProvider>()
    private val mockApiService = mockk<MikroTikApiService>()
    private val mockUserPreferencesRepository = mockk<UserPreferencesRepository>()
    private val callExecutor = MikroTikCallExecutor(mockServiceProvider)
    private val repository: ProbeStatusRepository = ProbeStatusRepositoryImpl(
        mockProbeRepository,
        callExecutor,
        mockUserPreferencesRepository
    )

    private val testProbe = ProbeConfig(
        ipAddress = "192.168.1.1",
        username = "admin",
        password = "password",
        testInterface = "ether1",
        isOnline = false,
        modelName = null,
        tdrSupported = false,
        isHttps = false
    )

    @Test
    fun `observeProbeStatus returns true when probe is online`() = runTest {
        // Given: Probe online (API risponde con successo)
        every { mockUserPreferencesRepository.probePollingInterval } returns flowOf(100L)
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any()) } returns listOf(
            SystemResource(boardName = "Test Board")
        )

        // When
        val result = repository.observeProbeStatus(testProbe).first()

        // Then
        assertTrue(result)
    }

    @Test
    fun `observeProbeStatus returns false when probe is offline`() = runTest {
        // Given: Probe offline (API lancia eccezione)
        every { mockUserPreferencesRepository.probePollingInterval } returns flowOf(100L)
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any()) } throws HttpException(
            mockk(relaxed = true)
        )

        // When
        val result = repository.observeProbeStatus(testProbe).first()

        // Then
        assertFalse(result)
    }

    // NOTE: Multi-probe legacy behaviour tests removed for single-probe "new-only" target.
}

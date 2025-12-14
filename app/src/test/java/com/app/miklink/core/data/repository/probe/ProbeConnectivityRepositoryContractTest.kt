package com.app.miklink.core.data.repository.probe

import android.content.Context
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.data.remote.mikrotik.dto.EthernetInterface
import com.app.miklink.core.data.remote.mikrotik.dto.ProplistRequest
import com.app.miklink.core.data.remote.mikrotik.dto.SystemResource
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.core.data.repository.ProbeCheckResult
import com.app.miklink.data.repositoryimpl.mikrotik.ProbeConnectivityRepositoryImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

/**
 * Contract test per ProbeConnectivityRepository.
 * Verifica il comportamento di verifica connessione e validazione configurazione sonda.
 */
class ProbeConnectivityRepositoryContractTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockServiceProvider = mockk<MikroTikServiceProvider>()
    private val mockApiService = mockk<MikroTikApiService>()
    private val repository: ProbeConnectivityRepository = ProbeConnectivityRepositoryImpl(
        mockContext,
        mockServiceProvider
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
    fun `checkProbeConnection returns Success with boardName and interfaces`() = runTest {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        // Given: Connessione riuscita
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any<ProplistRequest>()) } returns listOf(
            SystemResource(boardName = "RB4011")
        )
        coEvery { mockApiService.getEthernetInterfaces() } returns listOf(
            EthernetInterface(name = "ether1"),
            EthernetInterface(name = "ether2")
        )

        // When
        val result = repository.checkProbeConnection(testProbe)

        // Then
        assertTrue(result is ProbeCheckResult.Success)
        val success = result as ProbeCheckResult.Success
        assertEquals("RB4011", success.boardName)
        assertEquals(2, success.interfaces.size)
        assertTrue(success.interfaces.contains("ether1"))
        assertTrue(success.interfaces.contains("ether2"))
    }

    @Test
    fun `checkProbeConnection returns Error when connection fails`() = runTest {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        // Given: Connessione fallita
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any<ProplistRequest>()) } throws HttpException(
            mockk(relaxed = true)
        )
        every { mockContext.getString(any()) } returns "Connection failed"

        // When
        val result = repository.checkProbeConnection(testProbe)

        // Then
        assertTrue(result is ProbeCheckResult.Error)
        val error = result as ProbeCheckResult.Error
        assertNotNull(error.message)
    }

    @Test
    fun `checkProbeConnection returns Success with Unknown Board when boardName is missing`() = runTest {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        // Given: Risposta API senza board-name
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any<ProplistRequest>()) } returns emptyList()
        coEvery { mockApiService.getEthernetInterfaces() } returns listOf(
            EthernetInterface(name = "ether1")
        )

        // When
        val result = repository.checkProbeConnection(testProbe)

        // Then
        assertTrue(result is ProbeCheckResult.Success)
        val success = result as ProbeCheckResult.Success
        assertEquals("Unknown Board", success.boardName)
        assertEquals(1, success.interfaces.size)
    }
}


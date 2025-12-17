/*
 * Purpose: Contract tests for probe connectivity repository to verify board name/interface retrieval and error handling.
 * Inputs: Fake MikroTik API responses via mocked service provider and probe configuration.
 * Outputs: Assertions on ProbeCheckResult contents for success, errors, and missing board-name scenarios.
 * Notes: Ensures board names propagate correctly and handshake errors are surfaced as expected.
 */
package com.app.miklink.core.data.repository.probe

import android.content.Context
import com.app.miklink.R
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.dto.EthernetInterface
import com.app.miklink.data.remote.mikrotik.dto.ProplistRequest
import com.app.miklink.data.remote.mikrotik.dto.SystemResource
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
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
import javax.net.ssl.SSLHandshakeException

/**
 * Contract test per ProbeConnectivityRepository.
 * Verifica il comportamento di verifica connessione e validazione configurazione sonda.
 */
class ProbeConnectivityRepositoryContractTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockServiceProvider = mockk<MikroTikServiceProvider>()
    private val mockApiService = mockk<MikroTikApiService>()
    private val callExecutor = MikroTikCallExecutor(mockServiceProvider)
    private val repository: ProbeConnectivityRepository = ProbeConnectivityRepositoryImpl(
        mockContext,
        callExecutor
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
        every { android.util.Log.w(any(), any(), any()) } returns 0
        every { android.util.Log.isLoggable(any(), any()) } returns false
        // Given: Connessione riuscita
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any<ProplistRequest>()) } returns listOf(
            SystemResource(boardName = "RB4011")
        )
        coEvery { mockApiService.getEthernetInterfaces() } returns listOf(
            EthernetInterface(name = "ether1"),
            EthernetInterface(name = "ether2")
        )
        every { mockContext.getString(R.string.probe_verify_http_fallback_warning) } returns "Fallback HTTP"

        // When
        val result = repository.checkProbeConnection(testProbe)

        // Then
        assertTrue(result is ProbeCheckResult.Success)
        val success = result as ProbeCheckResult.Success
        assertEquals("RB4011", success.boardName)
        assertEquals(2, success.interfaces.size)
        assertTrue(success.interfaces.contains("ether1"))
        assertTrue(success.interfaces.contains("ether2"))
        assertFalse(success.didFallbackToHttp)
        assertFalse(success.effectiveIsHttps)
        assertNull(success.warning)
    }

    @Test
    fun `checkProbeConnection returns Error when connection fails`() = runTest {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.w(any(), any(), any()) } returns 0
        every { android.util.Log.isLoggable(any(), any()) } returns false
        // Given: Connessione fallita
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any<ProplistRequest>()) } throws HttpException(
            mockk(relaxed = true)
        )
        every { mockContext.getString(any()) } returns "Connection failed"
        every { mockContext.getString(R.string.probe_verify_http_fallback_warning) } returns "Fallback HTTP"

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
        every { android.util.Log.w(any(), any(), any()) } returns 0
        every { android.util.Log.isLoggable(any(), any()) } returns false
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

    @Test
    fun `checkProbeConnection picks first available boardName even if earlier entries missing`() = runTest {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.w(any(), any(), any()) } returns 0
        every { android.util.Log.isLoggable(any(), any()) } returns false
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any<ProplistRequest>()) } returns listOf(
            SystemResource(boardName = null),
            SystemResource(boardName = "RB5000")
        )
        coEvery { mockApiService.getEthernetInterfaces() } returns listOf(EthernetInterface(name = "ether1"))

        val result = repository.checkProbeConnection(testProbe)

        assertTrue(result is ProbeCheckResult.Success)
        val success = result as ProbeCheckResult.Success
        assertEquals("RB5000", success.boardName)
    }

    @Test
    fun `checkProbeConnection falls back to HTTP when HTTPS handshake fails`() = runTest {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.isLoggable(any(), any()) } returns false
        val httpsProbe = testProbe.copy(isHttps = true)
        val mockHttpsApi = mockk<MikroTikApiService>()
        val mockHttpApi = mockk<MikroTikApiService>()
        every { mockServiceProvider.build(match { it.isHttps }) } returns mockHttpsApi
        every { mockServiceProvider.build(match { !it.isHttps }) } returns mockHttpApi
        coEvery { mockHttpsApi.getSystemResource(any<ProplistRequest>()) } throws SSLHandshakeException("protocol_version")
        coEvery { mockHttpApi.getSystemResource(any<ProplistRequest>()) } returns listOf(
            SystemResource(boardName = "RB4011")
        )
        coEvery { mockHttpApi.getEthernetInterfaces() } returns listOf(EthernetInterface(name = "ether1"))
        every { mockContext.getString(R.string.probe_verify_http_fallback_warning) } returns "HTTPS failed, used HTTP"

        val result = repository.checkProbeConnection(httpsProbe)

        assertTrue(result is ProbeCheckResult.Success)
        val success = result as ProbeCheckResult.Success
        assertEquals("RB4011", success.boardName)
        assertEquals(listOf("ether1"), success.interfaces)
        assertTrue(success.didFallbackToHttp)
        assertFalse(success.effectiveIsHttps)
        assertEquals("HTTPS failed, used HTTP", success.warning)
    }

    @Test
    fun `checkProbeConnection surfaces TLS guidance if fallback fails`() = runTest {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.isLoggable(any(), any()) } returns false
        val httpsProbe = testProbe.copy(isHttps = true)
        val mockHttpsApi = mockk<MikroTikApiService>()
        val mockHttpApi = mockk<MikroTikApiService>()
        every { mockServiceProvider.build(match { it.isHttps }) } returns mockHttpsApi
        every { mockServiceProvider.build(match { !it.isHttps }) } returns mockHttpApi
        coEvery { mockHttpsApi.getSystemResource(any<ProplistRequest>()) } throws SSLHandshakeException("protocol_version")
        coEvery { mockHttpApi.getSystemResource(any<ProplistRequest>()) } throws HttpException(mockk(relaxed = true))
        every { mockContext.getString(R.string.error_probe_connection_unknown) } returns "Fallback"
        every { mockContext.getString(R.string.error_probe_connection_tls_handshake) } returns "TLS handshake failed guidance"
        every { mockContext.getString(R.string.probe_verify_http_fallback_warning) } returns "Fallback warning"

        val result = repository.checkProbeConnection(httpsProbe)

        assertTrue(result is ProbeCheckResult.Error)
        val error = result as ProbeCheckResult.Error
        assertEquals("TLS handshake failed guidance", error.message)
    }
}

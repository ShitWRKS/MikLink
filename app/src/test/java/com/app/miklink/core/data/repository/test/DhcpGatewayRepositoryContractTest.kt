/*
 * Purpose: Contract tests for DHCP gateway resolution using the centralized MikroTikCallExecutor path.
 * Inputs: ProbeConfig instances, mocked MikroTikServiceProvider/ApiService, and DhcpGatewayRepositoryImpl.
 * Outputs: Assertions on resolved gateways or null when unavailable or failing.
 * Notes: Keeps a single MikroTik transport path per ADR-0002 while validating repository behavior.
 */
package com.app.miklink.core.data.repository.test

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.dto.DhcpClientStatus
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.data.repository.mikrotik.MikroTikDhcpGatewayRepository
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

/**
 * Contract test per DhcpGatewayRepository.
 * Verifica il comportamento di risoluzione del gateway DHCP.
 */
class DhcpGatewayRepositoryContractTest {

    private val mockServiceProvider = mockk<MikroTikServiceProvider>()
    private val mockApiService = mockk<MikroTikApiService>()
    private val callExecutor = MikroTikCallExecutor(mockServiceProvider)
    private val repository: DhcpGatewayRepository = MikroTikDhcpGatewayRepository(callExecutor)

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

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.isLoggable(any(), any()) } returns false
    }

    @Test
    fun `getGatewayForInterface returns gateway when DHCP client is bound`() = runBlocking {
        // Given: DHCP client bound con gateway
        val dhcpStatus = DhcpClientStatus(
            id = "1",
            disabled = "false",
            status = "bound",
            address = "192.168.1.100",
            gateway = "192.168.1.1",
            dns = "8.8.8.8"
        )
        coEvery { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getDhcpClientStatus("ether1") } returns listOf(dhcpStatus)

        // When
        val result = repository.getGatewayForInterface(testProbe, "ether1")

        // Then
        assertEquals("192.168.1.1", result)
    }

    @Test
    fun `getGatewayForInterface returns null when DHCP client has no gateway`() = runBlocking {
        // Given: DHCP client bound ma senza gateway
        val dhcpStatus = DhcpClientStatus(
            id = "1",
            disabled = "false",
            status = "bound",
            address = "192.168.1.100",
            gateway = null,
            dns = "8.8.8.8"
        )
        coEvery { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getDhcpClientStatus("ether1") } returns listOf(dhcpStatus)

        // When
        val result = repository.getGatewayForInterface(testProbe, "ether1")

        // Then
        assertNull(result)
    }

    @Test
    fun `getGatewayForInterface returns null when no DHCP client exists`() = runBlocking {
        // Given: Nessun client DHCP
        coEvery { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getDhcpClientStatus("ether1") } returns emptyList()

        // When
        val result = repository.getGatewayForInterface(testProbe, "ether1")

        // Then
        assertNull(result)
    }

    @Test
    fun `getGatewayForInterface returns null when API call fails`() = runBlocking {
        // Given: Errore API
        coEvery { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getDhcpClientStatus("ether1") } throws Exception("Network error")

        // When
        val result = repository.getGatewayForInterface(testProbe, "ether1")

        // Then
        assertNull(result)
    }
}


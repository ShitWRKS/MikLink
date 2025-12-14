package com.app.miklink.core.data.repository.test

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.dto.DhcpClientStatus
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.data.repositoryimpl.mikrotik.DhcpGatewayRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Contract test per DhcpGatewayRepository.
 * Verifica il comportamento di risoluzione del gateway DHCP.
 */
class DhcpGatewayRepositoryContractTest {

    private val mockServiceProvider = mockk<MikroTikServiceProvider>()
    private val mockApiService = mockk<MikroTikApiService>()
    private val repository: DhcpGatewayRepository = DhcpGatewayRepositoryImpl(mockServiceProvider)

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


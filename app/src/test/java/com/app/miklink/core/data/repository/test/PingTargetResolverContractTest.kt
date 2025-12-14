package com.app.miklink.core.data.repository.test

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.data.repositoryimpl.PingTargetResolverImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Contract test per PingTargetResolver.
 * Verifica la risoluzione dei target ping, incluso il caso DHCP_GATEWAY.
 */
class PingTargetResolverContractTest {

    private val mockDhcpGatewayRepository = mockk<DhcpGatewayRepository>()
    private val resolver: PingTargetResolver = PingTargetResolverImpl(mockDhcpGatewayRepository)

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

    private val testClient = Client(
        clientId = 1,
        companyName = "Test Client",
        location = "Office",
        notes = null,
        networkMode = NetworkMode.DHCP,
        staticIp = null,
        staticSubnet = null,
        staticGateway = null,
        staticCidr = null,
        minLinkRate = "0",
        socketPrefix = "s",
        socketSuffix = "",
        socketSeparator = "-",
        socketNumberPadding = 2,
        nextIdNumber = 1,
        speedTestServerAddress = null,
        speedTestServerUser = null,
        speedTestServerPassword = null
    )

    private val testProfile = TestProfile(
        profileId = 1,
        profileName = "Test Profile",
        profileDescription = null,
        runLinkStatus = true,
        runTdr = false,
        runLldp = false,
        runPing = true,
        runSpeedTest = false,
        pingTarget1 = "8.8.8.8",
        pingTarget2 = null,
        pingTarget3 = null,
        pingCount = 4
    )

    @Test
    fun `resolve returns input when not DHCP_GATEWAY`() = runBlocking {
        // Given: Target normale
        val input = "8.8.8.8"

        // When
        val result = resolver.resolve(testProbe, testClient, testProfile, input)

        // Then
        assertEquals("8.8.8.8", result)
    }

    @Test
    fun `resolve returns gateway when DHCP_GATEWAY is resolved`() = runBlocking {
        // Given: Target DHCP_GATEWAY e gateway disponibile
        val input = "DHCP_GATEWAY"
        coEvery {
            mockDhcpGatewayRepository.getGatewayForInterface(testProbe, "ether1")
        } returns "192.168.1.1"

        // When
        val result = resolver.resolve(testProbe, testClient, testProfile, input)

        // Then
        assertEquals("192.168.1.1", result)
    }

    @Test
    fun `resolve returns input when DHCP_GATEWAY cannot be resolved`() = runBlocking {
        // Given: Target DHCP_GATEWAY ma gateway non disponibile
        val input = "DHCP_GATEWAY"
        coEvery {
            mockDhcpGatewayRepository.getGatewayForInterface(testProbe, "ether1")
        } returns null

        // When
        val result = resolver.resolve(testProbe, testClient, testProfile, input)

        // Then: Ritorna l'input originale quando il gateway non è disponibile
        assertEquals("DHCP_GATEWAY", result)
    }

    @Test
    fun `resolve is case insensitive for DHCP_GATEWAY`() = runBlocking {
        // Given: Target con case diverso
        val input = "dhcp_gateway"
        coEvery {
            mockDhcpGatewayRepository.getGatewayForInterface(testProbe, "ether1")
        } returns "192.168.1.1"

        // When
        val result = resolver.resolve(testProbe, testClient, testProfile, input)

        // Then
        assertEquals("192.168.1.1", result)
    }
}


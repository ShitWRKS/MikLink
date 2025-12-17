/*
 * Purpose: Ensure static network configuration validates inputs and stops before hitting the API when CIDR or gateway are invalid.
 * Inputs: NetworkConfigRepositoryImpl invoked with fake MikroTikApiService/RouteManager and static client settings.
 * Outputs: Verification that valid input triggers addIpAddress/addRoute, while invalid CIDR/gateway fail fast with no API calls.
 * Notes: Protects against RouterOS HTTP 400 caused by malformed keys or addresses.
 */
package com.app.miklink.data.repositoryimpl

import android.content.Context
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.dto.IpAddressAdd
import com.app.miklink.data.remote.mikrotik.dto.RouteAdd
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.data.repository.RouteManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test

class NetworkConfigRepositoryImplTest {

    private val context: Context = mockk(relaxed = true)
    private val api: MikroTikApiService = mockk(relaxed = true)
    private val serviceProvider: MikroTikServiceProvider = mockk()
    private val routeManager: RouteManager = mockk(relaxed = true)
    private val callExecutor = MikroTikCallExecutor(serviceProvider)
    private val repo = NetworkConfigRepositoryImpl(context, callExecutor, routeManager)

    private val probe = ProbeConfig(
        ipAddress = "192.168.0.10",
        username = "admin",
        password = "pwd",
        testInterface = "ether1",
        isHttps = false,
        isOnline = true,
        modelName = "hAP",
        tdrSupported = true
    )

    @Test
    fun `valid static config reaches api`() = runBlocking {
        coEvery { serviceProvider.build(probe) } returns api
        coEvery { api.getDhcpClientStatus(any()) } returns emptyList()
        coEvery { api.getIpAddresses(any()) } returns emptyList()

        val client = baseClient().copy(
            networkMode = NetworkMode.STATIC,
            staticCidr = "192.168.0.100/24",
            staticGateway = "192.168.0.1"
        )

        repo.applyClientNetworkConfig(probe, client, null)

        coVerify { api.addIpAddress(IpAddressAdd(address = "192.168.0.100/24", `interface` = "ether1")) }
        coVerify { api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = "192.168.0.1", comment = "MikLink_Auto")) }
    }

    @Test
    fun `invalid cidr fails fast`() = runBlocking {
        coEvery { serviceProvider.build(probe) } returns api
        coEvery { api.getDhcpClientStatus(any()) } returns emptyList()
        coEvery { api.getIpAddresses(any()) } returns emptyList()

        val client = baseClient().copy(
            networkMode = NetworkMode.STATIC,
            staticCidr = "192.168.0.100",
            staticGateway = "192.168.0.1"
        )

        try {
            repo.applyClientNetworkConfig(probe, client, null)
            fail("Expected IllegalArgumentException for invalid CIDR")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        coVerify(exactly = 0) { api.addIpAddress(any<IpAddressAdd>()) }
        coVerify(exactly = 0) { api.addRoute(any<RouteAdd>()) }
    }

    @Test
    fun `invalid gateway fails fast`() = runBlocking {
        coEvery { serviceProvider.build(probe) } returns api
        coEvery { api.getDhcpClientStatus(any()) } returns emptyList()
        coEvery { api.getIpAddresses(any()) } returns emptyList()

        val client = baseClient().copy(
            networkMode = NetworkMode.STATIC,
            staticCidr = "192.168.0.100/24",
            staticGateway = "not-an-ip"
        )

        try {
            repo.applyClientNetworkConfig(probe, client, null)
            fail("Expected IllegalArgumentException for invalid gateway")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        coVerify(exactly = 0) { api.addIpAddress(any<IpAddressAdd>()) }
        coVerify(exactly = 0) { api.addRoute(any<RouteAdd>()) }
    }

    private fun baseClient() = Client(
        clientId = 1L,
        companyName = "Acme",
        location = null,
        notes = null,
        networkMode = NetworkMode.DHCP,
        staticIp = null,
        staticSubnet = null,
        staticGateway = null,
        staticCidr = null,
        minLinkRate = "",
        socketPrefix = "PT",
        socketSuffix = "B",
        socketSeparator = "-",
        socketNumberPadding = 2,
        nextIdNumber = 1,
        speedTestServerAddress = null,
        speedTestServerUser = null,
        speedTestServerPassword = null
    )
}

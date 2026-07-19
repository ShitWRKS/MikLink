/*
 * Purpose: Ensure static network configuration validates inputs and stops before hitting the API when CIDR or gateway are invalid.
 * Inputs: MikroTikNetworkConfigRepository invoked with fake MikroTikApiService/RouteManager and static client settings.
 * Outputs: Verification that valid input triggers addIpAddress/addRoute, while invalid CIDR/gateway fail fast with no API calls.
 * Notes: Protects against RouterOS HTTP 400 caused by malformed keys or addresses.
 */
package com.app.miklink.data.repository.mikrotik

import android.content.Context
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TdrCapability
import com.app.miklink.data.remote.mikrotik.dto.IpAddressAdd
import com.app.miklink.data.remote.mikrotik.dto.RouteAdd
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.data.remote.mikrotik.service.RouterOsResponseDecoder
import com.squareup.moshi.Moshi
import com.app.miklink.data.repository.RouteManager
import com.app.miklink.data.repository.mikrotik.MikroTikNetworkConfigRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class NetworkConfigRepositoryTest {

    @Before
    fun mockAndroidLog() {
        mockkStatic("android.util.Log")
        every { android.util.Log.isLoggable(any(), any()) } returns false
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    private val context: Context = mockk(relaxed = true)
    private val api: MikroTikApiService = mockk(relaxed = true)
    private val serviceProvider: MikroTikServiceProvider = mockk()
    private val routeManager: RouteManager = mockk(relaxed = true)
    private val decoder: RouterOsResponseDecoder = RouterOsResponseDecoder(Moshi.Builder().build())
    private val callExecutor = MikroTikCallExecutor(serviceProvider)
    private val repo = MikroTikNetworkConfigRepository(context, callExecutor, decoder, routeManager)

    private val probe = ProbeConfig(
        ipAddress = "192.168.0.10",
        username = "admin",
        password = "pwd",
        testInterface = "ether1",
        isHttps = false,
        isOnline = true,
        modelName = "hAP",
        tdrCapability = TdrCapability.SUPPORTED
    )

    @Test
    fun `valid static config reaches api`() = runBlocking {
        coEvery { serviceProvider.build(probe) } returns api
        coEvery { api.getDhcpClientStatus(any()) } returns Response.success(emptyList())
        coEvery { api.getIpAddresses(any()) } returns Response.success(emptyList())
        coEvery { api.addIpAddress(any()) } returns Response.success(null)
        coEvery { api.addRoute(any()) } returns Response.success(null)

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
        coEvery { api.getDhcpClientStatus(any()) } returns Response.success(emptyList())
        coEvery { api.getIpAddresses(any()) } returns Response.success(emptyList())

        val client = baseClient().copy(
            networkMode = NetworkMode.STATIC,
            staticCidr = "192.168.0.100",
            staticGateway = "192.168.0.1"
        )

        try {
            repo.applyClientNetworkConfig(probe, client, null)
            fail("Expected failure for invalid CIDR")
        } catch (e: Exception) {
            // Validation errors propagate through TestExecutionException wrapping the classified error.
            assertTrue(
                "Expected a TestExecutionException with message about CIDR, got $e",
                e is com.app.miklink.core.domain.test.model.TestExecutionException
                        && e.error.message?.contains("CIDR") == true
            )
        }

        coVerify(exactly = 0) { api.addIpAddress(any<IpAddressAdd>()) }
        coVerify(exactly = 0) { api.addRoute(any<RouteAdd>()) }
    }

    @Test
    fun `invalid gateway fails fast`() = runBlocking {
        coEvery { serviceProvider.build(probe) } returns api
        coEvery { api.getDhcpClientStatus(any()) } returns Response.success(emptyList())
        coEvery { api.getIpAddresses(any()) } returns Response.success(emptyList())

        val client = baseClient().copy(
            networkMode = NetworkMode.STATIC,
            staticCidr = "192.168.0.100/24",
            staticGateway = "not-an-ip"
        )

        try {
            repo.applyClientNetworkConfig(probe, client, null)
            fail("Expected failure for invalid gateway")
        } catch (e: Exception) {
            assertTrue(
                "Expected a TestExecutionException with message about gateway, got $e",
                e is com.app.miklink.core.domain.test.model.TestExecutionException
                        && e.error.message?.contains("gateway", ignoreCase = true) == true
            )
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

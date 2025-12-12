package com.app.miklink.data.repositoryimpl

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import com.app.miklink.core.data.remote.mikrotik.infra.MikroTikServiceFactory
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.core.data.repository.test.PingTargetResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Replicates the legacy resolveTargetIp logic without depending on AppRepository.
 *
 * Temporary bridge: remove once PingStep no longer needs AppRepository (target EPIC S6).
 */
class PingTargetResolverImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serviceFactory: MikroTikServiceFactory
) : PingTargetResolver {

    @Suppress("DEPRECATION")
    private fun findWifiNetwork(): Network? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.allNetworks.firstOrNull { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    private fun buildServiceFor(probe: ProbeConfig): MikroTikApiService {
        val wifiNetwork = findWifiNetwork()
        return serviceFactory.createService(probe, wifiNetwork?.socketFactory)
    }

    private suspend fun getDhcpGateway(probe: ProbeConfig, interfaceName: String): String? {
        return try {
            val api = buildServiceFor(probe)
            api.getDhcpClientStatus(interfaceName).firstOrNull()?.gateway
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun resolve(
        probe: ProbeConfig,
        client: Client,
        profile: TestProfile,
        input: String
    ): String {
        if (input.equals("DHCP_GATEWAY", ignoreCase = true)) {
            return getDhcpGateway(probe, probe.testInterface) ?: input
        }
        return input
    }
}



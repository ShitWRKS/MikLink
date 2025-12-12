package com.app.miklink.data.repositoryimpl.mikrotik

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.remote.mikrotik.dto.*
import com.app.miklink.core.data.remote.mikrotik.infra.MikroTikServiceFactory
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Implementazione di MikroTikTestRepository che usa MikroTikApiService.
 * Replica la logica di AppRepository_legacy per costruire il service con WiFi network binding.
 */
class MikroTikTestRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serviceFactory: MikroTikServiceFactory
) : MikroTikTestRepository {

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

    override suspend fun monitorEthernet(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): MonitorResponse = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        val results = api.getLinkStatus(MonitorRequest(numbers = interfaceName, once = once))
        results.lastOrNull() ?: throw IllegalStateException("No link status returned")
    }

    override suspend fun cableTest(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): CableTestResult = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        val results = api.runCableTest(CableTestRequest(numbers = interfaceName, duration = "5s"))
        // Filtrare risultati validi: con cable-pairs O con status positivo
        results.lastOrNull {
            it.cablePairs != null || it.status.lowercase() in listOf("ok", "open", "link-ok", "running")
        } ?: throw IllegalStateException("No valid cable test results found")
    }

    override suspend fun ping(
        probe: ProbeConfig,
        target: String,
        interfaceName: String?,
        count: Int
    ): List<PingResult> = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        api.runPing(PingRequest(address = target, `interface` = interfaceName, count = count.toString()))
    }

    override suspend fun neighbors(
        probe: ProbeConfig,
        interfaceName: String
    ): List<NeighborDetail> = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        api.getIpNeighbors(interfaceName)
    }

    override suspend fun speedTest(
        probe: ProbeConfig,
        serverAddress: String,
        username: String?,
        password: String?,
        duration: String
    ): SpeedTestResult = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        val requestBody = SpeedTestRequest(
            address = serverAddress,
            user = username ?: "admin",
            password = password ?: "",
            testDuration = duration
        )
        val response = api.runSpeedTest(requestBody)
        if (response.isSuccessful) {
            val body = response.body()
            val result = body?.lastOrNull { it.status == "done" } ?: body?.lastOrNull()
            result ?: throw IllegalStateException("Empty speed test response")
        } else {
            when (response.code()) {
                400 -> throw IllegalArgumentException("Bad request: ${response.message()}")
                401, 403 -> throw SecurityException("Authentication failed")
                else -> throw HttpException(response)
            }
        }
    }

    override suspend fun systemResource(probe: ProbeConfig): SystemResource = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        api.getSystemResource(ProplistRequest(listOf("board-name"))).firstOrNull()
            ?: throw IllegalStateException("No system resource returned")
    }
}


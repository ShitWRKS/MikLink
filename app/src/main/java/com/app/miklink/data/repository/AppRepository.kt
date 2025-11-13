package com.app.miklink.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.network.*
import com.app.miklink.utils.UiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

// Result wrapper for the probe check
sealed class ProbeCheckResult {
    data class Success(val boardName: String, val interfaces: List<String>) : ProbeCheckResult()
    data class Error(val message: String) : ProbeCheckResult()
}

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    val clientDao: ClientDao,
    val probeConfigDao: ProbeConfigDao,
    val testProfileDao: TestProfileDao,
    val reportDao: ReportDao,
    private val retrofitBuilder: Retrofit.Builder,
    private val baseOkHttpClient: OkHttpClient,
    private val authInterceptor: AuthInterceptor
) {

    private var apiService: MikroTikApiService? = null
    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    private suspend fun findWifiNetwork(): Network? = withContext(Dispatchers.IO) {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return@withContext activeNetwork
        }
        return@withContext null
    }

    suspend fun setProbe(probe: ProbeConfig) {
        authInterceptor.setCredentials(probe.username, probe.password)
        val protocol = if (probe.isHttps) "https://" else "http://"
        val baseUrl = "$protocol${probe.ipAddress}/"

        val wifiNetwork = findWifiNetwork()

        val dynamicClient = if (wifiNetwork != null) {
            baseOkHttpClient.newBuilder()
                .socketFactory(wifiNetwork.socketFactory)
                .build()
        } else {
            baseOkHttpClient
        }

        this.apiService = retrofitBuilder
            .baseUrl(baseUrl)
            .client(dynamicClient)
            .build()
            .create(MikroTikApiService::class.java)
    }

    suspend fun resolveTargetIp(target: String, interfaceName: String): String {
        if (target == "DHCP_GATEWAY") {
            return getDhcpGateway(interfaceName) ?: target
        }
        return target
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): UiState<T> {
        return withContext(Dispatchers.IO) {
            try {
                UiState.Success(apiCall.invoke())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    internal suspend fun getDhcpGateway(interfaceName: String): String? {
        return try {
            apiService?.getDhcpClientStatus(InterfaceNameRequest(interfaceName))?.firstOrNull()?.gateway
        } catch (e: Exception) {
            null
        }
    }

    fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean> = flow {
        setProbe(probe)
        while (true) {
            val isOnline = try {
                apiService?.getSystemResource(ProplistRequest(listOf("board-name")))?.isNotEmpty() == true
            } catch (e: HttpException) {
                false
            } catch (e: Exception) {
                false
            }
            emit(isOnline)
            delay(15000) // Check every 15 seconds
        }
    }

    suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult = withContext(Dispatchers.IO) {
        setProbe(probe)
        try {
            val resourceRequest = ProplistRequest(listOf("board-name"))
            val interfaceRequest = ProplistRequest(listOf("name"))
            val boardName = apiService!!.getSystemResource(resourceRequest).firstOrNull()?.boardName ?: "Unknown Board"
            val interfaces = apiService!!.getEthernetInterfaces(interfaceRequest).map { it.name }
            ProbeCheckResult.Success(boardName, interfaces)
        } catch (e: Exception) {
            ProbeCheckResult.Error(e.message ?: "An unknown error occurred while connecting to the probe.")
        }
    }

    // --- TEST FUNCTIONS ---

    suspend fun runCableTest(interfaceName: String): UiState<CableTestResult> = safeApiCall {
        apiService!!.runCableTest(CableTestRequest(interfaceName)).first()
    }

    suspend fun getLinkStatus(interfaceName: String): UiState<MonitorResponse> = safeApiCall {
        apiService!!.getLinkStatus(MonitorRequest(numbers = interfaceName, once = true)).first()
    }

    suspend fun getNeighborsForInterface(interfaceName: String): UiState<List<NeighborDetail>> = safeApiCall {
        val request = NeighborRequest(
            query = listOf("interface=$interfaceName"),
            proplist = listOf("identity", "interface-name", "system-caps-enabled", "discovered-by", "vlan-id", "voice-vlan-id", "poe-class")
        )
        apiService!!.getIpNeighbors(request)
    }

    suspend fun runPing(target: String, interfaceName: String): UiState<PingResult> = safeApiCall {
        val resolvedTarget = resolveTargetIp(target, interfaceName)
        apiService!!.runPing(PingRequest(address = resolvedTarget)).first()
    }

    suspend fun runTraceroute(target: String): UiState<TracerouteResult> = safeApiCall {
        apiService!!.runTraceroute(TracerouteRequest(address = target)).first()
    }

    // --- NETWORK CONFIGURATION ---

    suspend fun addVlan(name: String, vlanId: String, interfaceName: String): UiState<String> = safeApiCall {
        val response = apiService!!.addVlan(VlanRequest(name, vlanId, interfaceName))
        response.first()["ret"] ?: throw IllegalStateException("Could not get VLAN ID from response")
    }

    suspend fun removeVlan(vlanId: String): UiState<Unit> = safeApiCall {
        apiService!!.removeVlan(RemoveRequest(vlanId))
    }

    suspend fun addIpAddress(address: String, interfaceName: String): UiState<String> = safeApiCall {
        val response = apiService!!.addIpAddress(IpAddressRequest(address, interfaceName))
        response.first()["ret"] ?: throw IllegalStateException("Could not get IP Address ID from response")
    }

    suspend fun removeIpAddress(ipId: String): UiState<Unit> = safeApiCall {
        apiService!!.removeIpAddress(RemoveRequest(ipId))
    }
}
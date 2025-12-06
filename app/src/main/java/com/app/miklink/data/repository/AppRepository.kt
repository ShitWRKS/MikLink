package com.app.miklink.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.network.*
import com.app.miklink.data.network.dto.SpeedTestRequest
import com.app.miklink.data.network.dto.SpeedTestResult
import com.app.miklink.utils.UiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import java.net.ConnectException
import java.net.SocketTimeoutException
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
    private val baseOkHttpClient: OkHttpClient
) {

    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    // NUOVO: Sonda unica (post-refactor)
    val currentProbe: Flow<ProbeConfig?> = probeConfigDao.getSingleProbe()

    private suspend fun findWifiNetwork(): Network? = withContext(Dispatchers.IO) {
        // Return active network only if it's WIFI
        val active = connectivityManager.activeNetwork
        val activeCaps = active?.let { connectivityManager.getNetworkCapabilities(it) }
        if (activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            active
        } else {
            null
        }
    }

    private suspend fun buildServiceFor(probe: ProbeConfig): MikroTikApiService {
        val protocol = if (probe.isHttps) "https://" else "http://"
        val baseUrl = "$protocol${probe.ipAddress}/"

        val wifiNetwork = findWifiNetwork()

        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val req = original.newBuilder()
                .header("Authorization", Credentials.basic(probe.username, probe.password))
                .build()
            chain.proceed(req)
        }

        val clientBuilder = baseOkHttpClient.newBuilder()
            .addInterceptor(authInterceptor)

        // Note: loggingInterceptor già presente in baseOkHttpClient, non duplicare

        if (wifiNetwork != null) {
            clientBuilder.socketFactory(wifiNetwork.socketFactory)
        }

        val client = clientBuilder.build()

        return retrofitBuilder
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(MikroTikApiService::class.java)
    }

    suspend fun resolveTargetIp(probe: ProbeConfig, target: String, interfaceName: String): String {
        if (target.equals("DHCP_GATEWAY", ignoreCase = true)) {
            return getDhcpGateway(probe, interfaceName) ?: target
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

    internal suspend fun getDhcpGateway(probe: ProbeConfig, interfaceName: String): String? {
        return try {
            val api = buildServiceFor(probe)
            api.getDhcpClientStatus(interfaceName).firstOrNull()?.gateway
        } catch (_: Exception) {
            null
        }
    }

    fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean> = flow {
        while (true) {
            val isOnline = try {
                val api = buildServiceFor(probe)
                api.getSystemResource(ProplistRequest(listOf("board-name"))).isNotEmpty()
            } catch (_: HttpException) {
                false
            } catch (_: Exception) {
                false
            }
            emit(isOnline)
            delay(15000)
        }
    }

    suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult = withContext(Dispatchers.IO) {
        try {
            val api = buildServiceFor(probe)
            val boardName = api.getSystemResource(ProplistRequest(listOf("board-name"))).firstOrNull()?.boardName ?: "Unknown Board"
            val interfacesRaw = api.getEthernetInterfaces()
            android.util.Log.d("AppRepository", "checkProbeConnection: Ricevute ${interfacesRaw.size} interfacce dall'API")
            val interfaces = interfacesRaw.map {
                android.util.Log.d("AppRepository", "checkProbeConnection: Interface name = '${it.name}'")
                it.name
            }
            android.util.Log.d("AppRepository", "checkProbeConnection: Interfacce mappate: $interfaces")
            ProbeCheckResult.Success(boardName, interfaces)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "checkProbeConnection: Errore durante la verifica", e)
            ProbeCheckResult.Error(e.message ?: "An unknown error occurred while connecting to the probe.")
        }
    }

    // --- CONFIG RETE PER CLIENTE ---

    data class NetworkConfigFeedback(
        val mode: String,
        val interfaceName: String,
        val address: String?,
        val gateway: String?,
        val dns: String?,
        val message: String
    )

    suspend fun applyClientNetworkConfig(
        probe: ProbeConfig,
        client: Client,
        override: Client? = null // usa un Client temporaneo per override per-singolo-test
    ): UiState<NetworkConfigFeedback> = safeApiCall {
        val effective = override ?: client
        val api = buildServiceFor(probe)
        val iface = probe.testInterface

        // Helper: rimuovi IP statici su interfaccia
        suspend fun removeStaticAddressesOnInterface() {
            val addresses = api.getIpAddresses()
            addresses.filter { it.iface == iface }.forEach { entry ->
                entry.id?.let { api.removeIpAddress(NumbersRequest(it)) }
            }
        }
        // Helper: rimuovi default route duplicate
        suspend fun removeDefaultRoutes() {
            val routes = api.getRoutes()
            routes.filter { it.dstAddress == "0.0.0.0/0" }.forEach { r -> r.id?.let { api.removeRoute(NumbersRequest(it)) } }
        }


        if (effective.networkMode.equals("DHCP", true)) {
            // DHCP: verifica se già configurato e bound, altrimenti configura
            val existingDhcp = api.getDhcpClientStatus(iface).firstOrNull()

            // Se il client DHCP esiste ed è già bound, non fare nulla
            if (existingDhcp != null &&
                existingDhcp.disabled == "false" &&
                existingDhcp.status?.equals("bound", ignoreCase = true) == true) {
                // DHCP già configurato correttamente, ritorna lo stato attuale
                return@safeApiCall NetworkConfigFeedback(
                    mode = "DHCP",
                    interfaceName = iface,
                    address = existingDhcp.address,
                    gateway = existingDhcp.gateway,
                    dns = existingDhcp.dns,
                    message = "DHCP già configurato e attivo"
                )
            }

            // Altrimenti, configura il DHCP
            if (existingDhcp != null) {
                existingDhcp.id?.let { dhcpId ->
                    // Client esiste ma è disabilitato o non bound: riabilita
                    if (existingDhcp.disabled == "true") {
                        api.enableDhcpClient(NumbersRequest(dhcpId))
                    } else {
                        // Client abilitato ma non bound: disable/enable per refresh
                        api.disableDhcpClient(NumbersRequest(dhcpId))
                        delay(500)
                        api.enableDhcpClient(NumbersRequest(dhcpId))
                    }
                }
            } else {
                // Client non esiste: crea
                try {
                    api.addDhcpClient(DhcpClientAdd(`interface` = iface))
                } catch (e: Exception) {
                    // Se il client esiste già (race condition), recuperalo e abilitalo
                    if (e.message?.contains("already exists", ignoreCase = true) == true) {
                        delay(500)
                        val existingId = api.getDhcpClientStatus(iface).firstOrNull()?.id
                        if (existingId != null) {
                            api.enableDhcpClient(NumbersRequest(existingId))
                        } else {
                            throw e
                        }
                    } else {
                        throw e
                    }
                }
            }

            // Attendi lease (max 6 secondi)
            var lease: DhcpClientStatus? = null
            repeat(6) {
                val cur = api.getDhcpClientStatus(iface).firstOrNull()
                if (cur?.status.equals("bound", true)) { lease = cur; return@repeat }
                delay(1000)
            }
            val bound = lease ?: api.getDhcpClientStatus(iface).firstOrNull()
            NetworkConfigFeedback(
                mode = "DHCP",
                interfaceName = iface,
                address = bound?.address,
                gateway = bound?.gateway,
                dns = bound?.dns,
                message = if (bound?.status == "bound") "DHCP lease acquisita" else "DHCP non bound (verificare server DHCP)"
            )
        } else {
            // STATIC
            // Disabilita DHCP se presente
            val dhcpId = api.getDhcpClientStatus(iface).firstOrNull()?.id
            if (dhcpId != null) api.disableDhcpClient(NumbersRequest(dhcpId))
            removeStaticAddressesOnInterface()
            removeDefaultRoutes()

            val cidr = effective.staticCidr ?: buildString {
                val ip = effective.staticIp ?: ""
                val mask = effective.staticSubnet ?: ""
                if (ip.isNotBlank() && mask.isNotBlank()) append("$ip/$mask")
            }
            require(!cidr.isNullOrBlank()) { "Static CIDR non configurato" }

            api.addIpAddress(IpAddressAdd(address = cidr, `interface` = iface))
            val gw = effective.staticGateway ?: error("Gateway statico mancante")
            api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gw))

            NetworkConfigFeedback(
                mode = "STATIC",
                interfaceName = iface,
                address = cidr,
                gateway = gw,
dns = null,
                message = "Indirizzo statico configurato"
            )
        }
    }

    suspend fun getCurrentInterfaceIpConfig(probe: ProbeConfig): UiState<NetworkConfigFeedback> = safeApiCall {
        val api = buildServiceFor(probe)
        val iface = probe.testInterface
        val dhcp = api.getDhcpClientStatus(iface).firstOrNull()
        if (dhcp != null && dhcp.status?.equals("bound", true) == true) {
            NetworkConfigFeedback("DHCP", iface, dhcp.address, dhcp.gateway, dhcp.dns, "Lease attiva")
        } else {
            val addr = api.getIpAddresses().firstOrNull { it.iface == iface }
            val route = api.getRoutes().firstOrNull { it.dstAddress == "0.0.0.0/0" }
            NetworkConfigFeedback("STATIC", iface, addr?.address, route?.gateway, null, "Statico rilevato")
        }
    }

    // --- TEST FUNCTIONS ---

    suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult> {
        android.util.Log.d("TDR_DEBUG", "=== Cable-Test Request Start ===")
        // Probe.name removed — use generic label with IP for logs
        android.util.Log.d("TDR_DEBUG", "Sonda @ ${probe.ipAddress}")
        android.util.Log.d("TDR_DEBUG", "Interface: $interfaceName")
        android.util.Log.d("TDR_DEBUG", "TDR Supported: ${probe.tdrSupported}")

        val startTime = System.currentTimeMillis()

        return withContext(Dispatchers.IO) {
            try {
                val api = buildServiceFor(probe)
                android.util.Log.d("TDR_DEBUG", "API service built, calling cable-test...")

                val results = api.runCableTest(CableTestRequest(interfaceName))
                val elapsed = System.currentTimeMillis() - startTime

                android.util.Log.d("TDR_DEBUG", "Response received after ${elapsed}ms")
                android.util.Log.d("TDR_DEBUG", "Results count: ${results.size}")
                results.forEachIndexed { index, result ->
                    android.util.Log.d("TDR_DEBUG", "  [$index] Status: ${result.status}")
                    android.util.Log.d("TDR_DEBUG", "       Cable pairs: ${result.cablePairs?.size ?: 0}")
                    result.cablePairs?.forEach { pair ->
                        android.util.Log.d("TDR_DEBUG", "         - $pair")
                    }
                }

                // Filtrare risultati validi: con cable-pairs O con status positivo
                val finalResult = results.lastOrNull { 
                    it.cablePairs != null || it.status.lowercase() in listOf("ok", "open", "link-ok", "running")
                } ?: throw IllegalStateException("No valid cable test results found (no cable-pairs and no valid status)")

                UiState.Success(finalResult)

            } catch (e: SocketTimeoutException) {
                val elapsed = System.currentTimeMillis() - startTime
                android.util.Log.e("TDR_DEBUG", "TIMEOUT after ${elapsed}ms: ${e.message}", e)
                UiState.Error("Timeout durante cable-test (>${elapsed/1000}s). Il comando potrebbe richiedere più tempo su questo modello.")

            } catch (e: HttpException) {
                android.util.Log.e("TDR_DEBUG", "HTTP ERROR ${e.code()}: ${e.message()}", e)
                val errorBody = e.response()?.errorBody()?.string()
                android.util.Log.e("TDR_DEBUG", "Error body: $errorBody")

                when (e.code()) {
                    500 -> UiState.Error("Cable-Test non supportato da questo hardware MikroTik")
                    400 -> UiState.Error("Richiesta cable-test non valida: $errorBody")
                    else -> UiState.Error("Errore HTTP ${e.code()}: ${e.message()}")
                }

            } catch (e: Exception) {
                android.util.Log.e("TDR_DEBUG", "GENERIC ERROR: ${e::class.simpleName} - ${e.message}", e)
                UiState.Error("Errore cable-test: ${e.message ?: "Errore sconosciuto"}")
            }
        }
    }

    suspend fun getLinkStatus(probe: ProbeConfig, interfaceName: String): UiState<MonitorResponse> = safeApiCall {
        val api = buildServiceFor(probe)
        val results = api.getLinkStatus(MonitorRequest(numbers = interfaceName, once = true))
        results.lastOrNull() ?: throw IllegalStateException("No link status returned")
    }

    suspend fun getNeighborsForInterface(probe: ProbeConfig, interfaceName: String): UiState<List<NeighborDetail>> {
        android.util.Log.d("LLDP_DEBUG", "=== LLDP Request Start ===")
            android.util.Log.d("LLDP_DEBUG", "Sonda @ ${probe.ipAddress}")
        android.util.Log.d("LLDP_DEBUG", "Interface: $interfaceName")
        android.util.Log.d("LLDP_DEBUG", "Query parameter: interface=$interfaceName (sintassi corretta)")

        return try {
            val api = buildServiceFor(probe)
            val result = api.getIpNeighbors(interfaceName)

            // Normalizza: Retrofit potrebbe restituire List vuota se nessun neighbor
            val normalizedResult = result ?: emptyList()

            android.util.Log.d("LLDP_DEBUG", "Response: ${normalizedResult.size} neighbor(s) found")
            normalizedResult.forEachIndexed { index, neighbor ->
                android.util.Log.d("LLDP_DEBUG", "  [$index] Identity: ${neighbor.identity}")
                android.util.Log.d("LLDP_DEBUG", "       Interface: ${neighbor.interfaceName}")
                android.util.Log.d("LLDP_DEBUG", "       Discovered by: ${neighbor.discoveredBy}")
                android.util.Log.d("LLDP_DEBUG", "       Caps: ${neighbor.systemCaps}")
            }
            android.util.Log.d("LLDP_DEBUG", "=== LLDP Request End ===")

            UiState.Success(normalizedResult)
        } catch (e: Exception) {
            android.util.Log.e("LLDP_DEBUG", "ERRORE LLDP: ${e.message}", e)
            UiState.Error(e.message ?: "Errore sconosciuto LLDP/CDP")
        }
    }

    suspend fun runPing(probe: ProbeConfig, target: String, interfaceName: String, count: Int = 4): UiState<List<PingResult>> = safeApiCall {
        val resolvedTarget = resolveTargetIp(probe, target, interfaceName)
        if (resolvedTarget.equals("DHCP_GATEWAY", ignoreCase = true)) {
            throw IllegalStateException("DHCP gateway not resolved for interface $interfaceName")
        }
        val api = buildServiceFor(probe)
        api.runPing(PingRequest(address = resolvedTarget, `interface` = interfaceName, count = count.toString()))
    }

    suspend fun runSpeedTest(probe: ProbeConfig, client: Client): UiState<SpeedTestResult> {
        if (client.speedTestServerAddress.isNullOrBlank()) {
            return UiState.Error("Indirizzo server speed test non configurato.")
        }

        return try {
            val api = buildServiceFor(probe)

            // Crea l'oggetto Request con i parametri nel body (non nella query string)
            val requestBody = SpeedTestRequest(
                address = client.speedTestServerAddress,
                user = client.speedTestServerUser ?: "admin",
                password = client.speedTestServerPassword ?: "",
                testDuration = "5"
            )

            // Passa l'oggetto request nel body
            val response = api.runSpeedTest(requestBody)

            if (response.isSuccessful) {
                val body = response.body()
                val result = body?.lastOrNull { it.status == "done" } ?: body?.lastOrNull()
                if (result != null) {
                    UiState.Success(result)
                } else {
                    UiState.Error("Risposta vuota dal server.")
                }
            } else {
                when (response.code()) {
                    400 -> UiState.Error("Errore 400: Richiesta non valida. (Controlla i parametri)")
                    401, 403 -> UiState.Error("Errore 401: Username o Password del server errati.")
                    else -> UiState.Error("Errore API: ${response.code()} ${response.message()}")
                }
            }
        } catch (e: HttpException) {
            UiState.Error("Errore HTTP: ${e.message() ?: e.message}")
        } catch (e: SocketTimeoutException) {
            UiState.Error("Server non raggiungibile (Timeout).")
        } catch (e: ConnectException) {
            UiState.Error("Impossibile connettersi al server.")
        } catch (e: Exception) {
            UiState.Error("Errore sconosciuto: ${e.message}")
        }
    }


    // --- PROBE MONITORING ---

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>> =
        probeConfigDao.getAllProbes().flatMapLatest { probes ->
            if (probes.isEmpty()) return@flatMapLatest flowOf(emptyList())
            tickerFlow(10_000L).map {
                withContext(Dispatchers.IO) {
                    probes.map { probe ->
                        val isOnline = try {
                            val api = buildServiceFor(probe)
                            val result = api.getSystemResource(ProplistRequest(listOf("board-name")))
                            result.isNotEmpty()
                        } catch (e: Exception) {
                            android.util.Log.w("AppRepository", "Sonda @ ${probe.ipAddress} offline: ${e.message}")
                            false
                        }
                        ProbeStatusInfo(probe, isOnline)
                    }
                }
            }
        }

    // Convenience method to persist the single probe configuration
    suspend fun saveProbe(probe: ProbeConfig) {
        probeConfigDao.upsertSingle(probe)
    }
}

// Simple ticker flow
private fun tickerFlow(periodMs: Long): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(periodMs)
    }
}

// Probe status monitoring
data class ProbeStatusInfo(val probe: com.app.miklink.data.db.model.ProbeConfig, val isOnline: Boolean)

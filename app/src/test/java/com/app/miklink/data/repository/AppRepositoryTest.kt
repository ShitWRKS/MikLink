package com.app.miklink.data.repository

import android.content.Context
import android.net.ConnectivityManager
import app.cash.turbine.test
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.network.MikroTikApiService
import com.app.miklink.data.network.PingRequest
import com.app.miklink.data.network.PingResult
import com.app.miklink.data.network.ProplistRequest
import com.app.miklink.data.network.SystemResource
import com.app.miklink.utils.UiState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit test per AppRepository
 * Verifica l'orchestrazione tra DAO locali e API service remoto
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryTest {

    private lateinit var repository: AppRepository

    // Mock dei DAO
    private lateinit var mockClientDao: ClientDao
    private lateinit var mockProbeConfigDao: ProbeConfigDao
    private lateinit var mockTestProfileDao: TestProfileDao
    private lateinit var mockReportDao: ReportDao
    private lateinit var mockUserPreferencesRepository: UserPreferencesRepository

    // Mock dell'API Service
    private lateinit var mockApiService: MikroTikApiService

    // Mock di Context e dipendenze Android
    private lateinit var mockContext: Context
    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockRetrofitBuilder: Retrofit.Builder
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockRetrofit: Retrofit
    private lateinit var mockOkHttpBuilder: OkHttpClient.Builder


    @Before
    fun setup() {
        // Mock DAO
        mockClientDao = mockk(relaxed = true)
        mockProbeConfigDao = mockk(relaxed = true)
        mockTestProfileDao = mockk(relaxed = true)
        mockReportDao = mockk(relaxed = true)
        mockUserPreferencesRepository = mockk(relaxed = true)

        // Mock API Service
        mockApiService = mockk(relaxed = true)

        // Mock Android dependencies
        mockContext = mockk(relaxed = true)
        mockConnectivityManager = mockk(relaxed = true)
        mockOkHttpClient = mockk(relaxed = true)
        mockOkHttpBuilder = mockk(relaxed = true)
        mockRetrofit = mockk(relaxed = true)
        mockRetrofitBuilder = mockk(relaxed = true) {
            every { baseUrl(any<String>()) } returns this@mockk
            every { client(any()) } returns this@mockk
            every { build() } returns mockRetrofit
        }

        // OkHttp builder chain
        every { mockOkHttpClient.newBuilder() } returns mockOkHttpBuilder
        every { mockOkHttpBuilder.addInterceptor(any<okhttp3.Interceptor>()) } returns mockOkHttpBuilder
        every { mockOkHttpBuilder.build() } returns mockOkHttpClient
        // socketFactory sarà chiamato solo se rete WiFi trovata; ritorna il builder per sicurezza
        every { mockOkHttpBuilder.socketFactory(any()) } returns mockOkHttpBuilder

        // Mock Retrofit.create() per restituire il mockApiService
        every { mockRetrofit.create(MikroTikApiService::class.java) } returns mockApiService

        // Mock Context.getSystemService per ConnectivityManager
        every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        every { mockConnectivityManager.activeNetwork } returns null
        every { mockConnectivityManager.getNetworkCapabilities(any()) } returns null

        // NON creare il repository qui - sarà creato in ogni test dopo aver configurato i mock specifici
    }

    private fun createRepository(): AppRepository {
        return AppRepository(
            context = mockContext,
            clientDao = mockClientDao,
            probeConfigDao = mockProbeConfigDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            retrofitBuilder = mockRetrofitBuilder,
            baseOkHttpClient = mockOkHttpClient,
            userPreferencesRepository = mockUserPreferencesRepository
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    /**
     * Test 1: currentProbe (Flusso DAO)
     * Verifica che il repository esponga correttamente il Flow dal DAO
     */
    @Test
    fun `test currentProbe returns flow from DAO`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        // Mock DAO per emettere il testProbe
        every { mockProbeConfigDao.getSingleProbe() } returns flowOf(testProbe)

        // Crea il repository DOPO aver configurato il mock
        repository = createRepository()

        // Act & Assert
        repository.currentProbe.test {
            val result = awaitItem()
            assertEquals(testProbe, result)
            assertEquals(1L, result?.probeId)
            // name column removed; no assertion on name
            assertEquals("192.168.1.1", result?.ipAddress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 2: currentProbe con null
     * Verifica che il repository gestisca correttamente il caso di nessuna sonda
     */
    @Test
    fun `test currentProbe returns null when no probe exists`() = runTest {
        // Arrange
        every { mockProbeConfigDao.getSingleProbe() } returns flowOf(null)

        // Crea il repository DOPO aver configurato il mock
        repository = createRepository()

        // Act & Assert
        repository.currentProbe.test {
            val result = awaitItem()
            assertEquals(null, result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 3: runPing (Orchestrazione API)
     * Verifica che il repository chiami correttamente l'API con i parametri del profile
     */
    @Test
    fun `test runPing calls API with correct parameters from profile`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        val pingTarget = "8.8.8.8"
        val pingCount = 10
        val interfaceName = "ether1"

        val expectedPingResults = listOf(
            PingResult(
                avgRtt = "20ms",
                host = "8.8.8.8",
                maxRtt = "25ms",
                minRtt = "15ms",
                packetLoss = "0",
                received = "10",
                sent = "10",
                seq = "0",
                size = "56",
                time = "20ms",
                ttl = "64"
            )
        )

        // Mock API per restituire risultati di ping
        coEvery {
            mockApiService.runPing(
                PingRequest(
                    address = pingTarget,
                    `interface` = interfaceName,
                    count = pingCount.toString()
                )
            )
        } returns expectedPingResults

        // Crea il repository
        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = pingTarget,
            interfaceName = interfaceName,
            count = pingCount
        )

        // Assert
        assertTrue(result is UiState.Success)
        val successResult = result as UiState.Success
        assertEquals(expectedPingResults, successResult.data)
        assertEquals("20ms", successResult.data.first().avgRtt)
        assertEquals("10", successResult.data.first().received)

        // Verifica che l'API sia stata chiamata con i parametri corretti
        coVerify(exactly = 1) {
            mockApiService.runPing(
                match {
                    it.address == pingTarget &&
                    it.`interface` == interfaceName &&
                    it.count == pingCount.toString()
                }
            )
        }
    }

    /**
     * Test 4: runPing con target DHCP_GATEWAY
     * Verifica che il repository risolva correttamente il gateway DHCP
     */
    @Test
    fun `test runPing resolves DHCP_GATEWAY target`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        val interfaceName = "ether1"
        val resolvedGateway = "192.168.1.254"

        // Mock getDhcpGateway per restituire il gateway risolto
        // Nota: getDhcpGateway è internal, quindi mockiamo direttamente getDhcpClientStatus
        coEvery {
            mockApiService.getDhcpClientStatus(interfaceName)
        } returns listOf(
            mockk {
                every { gateway } returns resolvedGateway
            }
        )

        val expectedPingResults = listOf(
            PingResult(
                avgRtt = "5ms",
                host = resolvedGateway,
                maxRtt = "10ms",
                minRtt = "2ms",
                packetLoss = "0",
                received = "4",
                sent = "4",
                seq = "0",
                size = "56",
                time = "5ms",
                ttl = "64"
            )
        )

        coEvery {
            mockApiService.runPing(any())
        } returns expectedPingResults

        // Crea il repository
        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = "DHCP_GATEWAY",
            interfaceName = interfaceName,
            count = 4
        )

        // Assert
        assertTrue(result is UiState.Success)

        // Verifica che getDhcpClientStatus sia stato chiamato per risolvere il gateway
        coVerify { mockApiService.getDhcpClientStatus(interfaceName) }

        // Verifica che runPing sia stato chiamato con il gateway risolto
        coVerify {
            mockApiService.runPing(
                match { it.address == resolvedGateway }
            )
        }
    }

    /**
     * Test 5: runPing gestisce errori API
     * Verifica che il repository gestisca correttamente gli errori dall'API
     */
    @Test
    fun `test runPing returns error on API failure`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        val errorMessage = "Network timeout"

        coEvery {
            mockApiService.runPing(any())
        } throws Exception(errorMessage)

        // Crea il repository
        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = "8.8.8.8",
            interfaceName = "ether1",
            count = 4
        )

        // Assert
        assertTrue(result is UiState.Error)
        val errorResult = result as UiState.Error
        assertEquals(errorMessage, errorResult.message)
    }

    /**
     * Test 6: checkProbeConnection (Orchestrazione API)
     * Verifica che il repository verifichi correttamente la connessione alla sonda
     */
    @Test
    fun `test checkProbeConnection returns success with board info`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        val expectedBoardName = "RB750Gr3"
        val expectedInterfaces = listOf("ether1", "ether2", "ether3", "ether4", "ether5")

        // Mock API responses
        coEvery {
            mockApiService.getSystemResource(ProplistRequest(listOf("board-name")))
        } returns listOf(
            SystemResource(boardName = expectedBoardName)
        )

        coEvery {
            mockApiService.getEthernetInterfaces()
        } returns expectedInterfaces.map { name ->
            mockk {
                every { this@mockk.name } returns name
            }
        }

        // Crea il repository
        repository = createRepository()

        // Act
        val result = repository.checkProbeConnection(testProbe)

        // Assert
        assertTrue(result is ProbeCheckResult.Success)
        val successResult = result as ProbeCheckResult.Success
        assertEquals(expectedBoardName, successResult.boardName)
        assertEquals(expectedInterfaces, successResult.interfaces)

        // Verifica che entrambe le API siano state chiamate
        coVerify { mockApiService.getSystemResource(ProplistRequest(listOf("board-name"))) }
        coVerify { mockApiService.getEthernetInterfaces() }
    }

    /**
     * Test 7: checkProbeConnection gestisce errori
     * Verifica che il repository gestisca errori di connessione alla sonda
     */
    @Test
    fun `test checkProbeConnection returns error on connection failure`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = false,
            modelName = null,
            tdrSupported = false,
            isHttps = false
        )

        val errorMessage = "Connection refused"

        coEvery {
            mockApiService.getSystemResource(any())
        } throws Exception(errorMessage)

        // Crea il repository
        repository = createRepository()

        // Act
        val result = repository.checkProbeConnection(testProbe)

        // Assert
        assertTrue(result is ProbeCheckResult.Error)
        val errorResult = result as ProbeCheckResult.Error
        assertEquals(errorMessage, errorResult.message)
    }

    /**
     * Test 8: saveProbe (Persistenza DAO)
     * Verifica che il repository salvi correttamente la sonda tramite il DAO
     */
    @Test
    fun `test saveProbe passes to DAO`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 42L,
            ipAddress = "10.0.0.10",
            username = "u",
            password = "p",
            testInterface = "ether3",
            isOnline = false,
            modelName = null,
            tdrSupported = false,
            isHttps = true
        )
        coEvery { mockProbeConfigDao.upsertSingle(testProbe) } returns Unit

        // Create repository
        repository = createRepository()

        // Act
        repository.saveProbe(testProbe)

        // Assert
        coVerify(exactly = 1) { mockProbeConfigDao.upsertSingle(testProbe) }
    }

    // ========== Test per safeApiCall (Gestione Eccezioni di Rete) ==========

    /**
     * Test 9: safeApiCall gestisce SocketTimeoutException
     * Verifica che il repository gestisca correttamente i timeout di rete
     */
    @Test
    fun `test safeApiCall handles SocketTimeoutException`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        coEvery {
            mockApiService.runPing(any())
        } throws SocketTimeoutException("Connection timeout")

        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = "8.8.8.8",
            interfaceName = "ether1",
            count = 4
        )

        // Assert
        assertTrue(result is UiState.Error)
        val errorResult = result as UiState.Error
        assertTrue(errorResult.message.contains("timeout", ignoreCase = true))
    }

    /**
     * Test 10: safeApiCall gestisce HttpException (500)
     * Verifica che il repository gestisca correttamente errori HTTP generici
     */
    @Test
    fun `test safeApiCall handles HttpException with 500 error`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        val mockResponse = retrofit2.Response.error<String>(
            500,
            "Internal Server Error".toResponseBody(null)
        )

        val httpException = HttpException(mockResponse)

        coEvery {
            mockApiService.runPing(any())
        } throws httpException

        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = "8.8.8.8",
            interfaceName = "ether1",
            count = 4
        )

        // Assert
        assertTrue(result is UiState.Error)
        val errorResult = result as UiState.Error
        assertTrue(errorResult.message.isNotEmpty())
    }

    /**
     * Test 11: safeApiCall gestisce ConnectException
     * Verifica che il repository gestisca correttamente errori di connessione
     */
    @Test
    fun `test safeApiCall handles ConnectException`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        coEvery {
            mockApiService.runPing(any())
        } throws ConnectException("Connection refused")

        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = "8.8.8.8",
            interfaceName = "ether1",
            count = 4
        )

        // Assert
        assertTrue(result is UiState.Error)
        val errorResult = result as UiState.Error
        assertTrue(errorResult.message.contains("refused", ignoreCase = true))
    }

    /**
     * Test 12: safeApiCall gestisce UnknownHostException
     * Verifica che il repository gestisca correttamente errori di risoluzione DNS
     */
    @Test
    fun `test safeApiCall handles UnknownHostException`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        coEvery {
            mockApiService.runPing(any())
        } throws UnknownHostException("Host not found")

        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = "8.8.8.8",
            interfaceName = "ether1",
            count = 4
        )

        // Assert
        assertTrue(result is UiState.Error)
        val errorResult = result as UiState.Error
        assertTrue(errorResult.message.contains("not found", ignoreCase = true) || errorResult.message.contains("Host", ignoreCase = true))
    }

    /**
     * Test 13: safeApiCall gestisce HttpException con 404
     * Verifica che il repository gestisca correttamente errori HTTP 404
     */
    @Test
    fun `test safeApiCall handles HttpException with 404 error`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        val mockResponse = retrofit2.Response.error<String>(
            404,
            "Not Found".toResponseBody(null)
        )

        val httpException = HttpException(mockResponse)

        coEvery {
            mockApiService.runPing(any())
        } throws httpException

        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = "8.8.8.8",
            interfaceName = "ether1",
            count = 4
        )

        // Assert
        assertTrue(result is UiState.Error)
        val errorResult = result as UiState.Error
        assertTrue(errorResult.message.isNotEmpty())
    }

    /**
     * Test 14: safeApiCall ritorna Success per operazione riuscita
     * Verifica che il repository gestisca correttamente chiamate API di successo
     */
    @Test
    fun `test safeApiCall returns Success for successful API call`() = runTest {
        // Arrange
        val testProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        val expectedPingResults = listOf(
            PingResult(
                avgRtt = "10ms",
                host = "8.8.8.8",
                maxRtt = "15ms",
                minRtt = "5ms",
                packetLoss = "0",
                received = "4",
                sent = "4",
                seq = "0",
                size = "56",
                time = "10ms",
                ttl = "64"
            )
        )

        coEvery {
            mockApiService.runPing(any())
        } returns expectedPingResults

        repository = createRepository()

        // Act
        val result = repository.runPing(
            probe = testProbe,
            target = "8.8.8.8",
            interfaceName = "ether1",
            count = 4
        )

        // Assert
        assertTrue(result is UiState.Success)
        val successResult = result as UiState.Success
        assertEquals(expectedPingResults, successResult.data)
        assertEquals("10ms", successResult.data.first().avgRtt)
    }
}

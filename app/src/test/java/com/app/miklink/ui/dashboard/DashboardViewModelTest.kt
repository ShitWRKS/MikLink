package com.app.miklink.ui.dashboard

import app.cash.turbine.test
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.repository.AppRepository
import com.app.miklink.data.repository.IdNumberingStrategy
import com.app.miklink.data.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var mockClientDao: ClientDao
    private lateinit var mockTestProfileDao: TestProfileDao
    private lateinit var mockReportDao: ReportDao
    private lateinit var mockRepository: AppRepository
    private lateinit var mockUserPreferencesRepository: UserPreferencesRepository

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock dei DAO
        mockClientDao = mockk(relaxed = true) {
            every { getAllClients() } returns flowOf(emptyList())
        }

        mockTestProfileDao = mockk(relaxed = true) {
            every { getAllProfiles() } returns flowOf(emptyList())
        }

        mockReportDao = mockk(relaxed = true) {
            coEvery { getLastReportForClient(any()) } returns null
            every { getReportsForClient(any()) } returns flowOf(emptyList())
        }

        // Mock del Repository (sarà configurato per ogni test)
        mockRepository = mockk(relaxed = true)

        // Mock UserPreferencesRepository con strategia default
        mockUserPreferencesRepository = mockk(relaxed = true) {
            every { idNumberingStrategy } returns flowOf(IdNumberingStrategy.CONTINUOUS_INCREMENT)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test 1: Sonda Online
     * Verifica che quando il repository emette una ProbeConfig con isOnline = true,
     * il ViewModel esponga correttamente currentProbe e isProbeOnline = true
     */
    @Test
    fun `test currentProbe online should set isProbeOnline to true`() = runTest {
        // Arrange
        val onlineProbe = ProbeConfig(
            probeId = 1L,
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "pass",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        // Mock repository.currentProbe
        every { mockRepository.currentProbe } returns flowOf(onlineProbe)

        // Mock repository.observeProbeStatus (deve ritornare true per sonda online)
        every { mockRepository.observeProbeStatus(onlineProbe) } returns flowOf(true)

        // Act - Crea il ViewModel
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )


        // Assert
        viewModel.currentProbe.test {
            val probeValue = awaitItem()
            assertEquals(onlineProbe, probeValue)
            assertEquals(1L, probeValue?.probeId)
            // name removed from ProbeConfig; omit name assertion
            assertEquals(true, probeValue?.isOnline)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.isProbeOnline.test {
            val isOnline = awaitItem()
            assertEquals(true, isOnline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 2: Sonda Offline
     * Verifica che quando il repository emette una ProbeConfig con isOnline = false,
     * il ViewModel esponga correttamente currentProbe e isProbeOnline = false
     */
    @Test
    fun `test currentProbe offline should set isProbeOnline to false`() = runTest {
        // Arrange
        val offlineProbe = ProbeConfig(
            probeId = 2L,
            ipAddress = "192.168.1.2",
            username = "admin",
            password = "pass",
            testInterface = "ether2",
            isOnline = false,
            modelName = "CCR1009",
            tdrSupported = false,
            isHttps = true
        )

        // Mock repository.currentProbe
        every { mockRepository.currentProbe } returns flowOf(offlineProbe)

        // Mock repository.observeProbeStatus (deve ritornare false per sonda offline)
        every { mockRepository.observeProbeStatus(offlineProbe) } returns flowOf(false)

        // Act - Crea il ViewModel
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )


        // Assert
        viewModel.currentProbe.test {
            val probeValue = awaitItem()
            assertEquals(offlineProbe, probeValue)
            assertEquals(2L, probeValue?.probeId)
            // name removed from ProbeConfig; omit name assertion
            assertEquals(false, probeValue?.isOnline)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.isProbeOnline.test {
            val isOnline = awaitItem()
            assertEquals(false, isOnline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 3: Nessuna Sonda
     * Verifica che quando il repository emette null (nessuna sonda configurata),
     * il ViewModel esponga currentProbe = null e isProbeOnline = false
     */
    @Test
    fun `test null currentProbe should set isProbeOnline to false`() = runTest {
        // Arrange
        // Mock repository.currentProbe (nessuna sonda configurata)
        every { mockRepository.currentProbe } returns flowOf(null)

        // Act - Crea il ViewModel
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )


        // Assert
        viewModel.currentProbe.test {
            val probeValue = awaitItem()
            assertNull(probeValue)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.isProbeOnline.test {
            val isOnline = awaitItem()
            assertEquals(false, isOnline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 4: Continuous Increment Strategy - First Test
     * Verifica che con strategia CONTINUOUS_INCREMENT, il primo test abbia ID = 1
     */
    @Test
    fun `test continuous increment strategy - first test should have ID 1`() = runTest {
        // Arrange
        val testClient = Client(
            clientId = 1L,
            companyName = "Test Client",
            location = "Milano",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            socketPrefix = "TST",
            nextIdNumber = 1
        )

        every { mockRepository.currentProbe } returns flowOf(null)
        every { mockUserPreferencesRepository.idNumberingStrategy } returns flowOf(IdNumberingStrategy.CONTINUOUS_INCREMENT)
        every { mockReportDao.getReportsForClient(1L) } returns flowOf(emptyList())

        // Act
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )

        viewModel.selectedClient.value = testClient

        // Assert
        viewModel.socketName.test {
            val socketName = awaitItem()
            assertEquals("TST001", socketName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 5: Continuous Increment Strategy - After 5 Tests
     * Verifica che con strategia CONTINUOUS_INCREMENT, dopo 5 test il prossimo sia 6
     */
    @Test
    fun `test continuous increment strategy - after 5 tests next should be 6`() = runTest {
        // Arrange
        val testClient = Client(
            clientId = 2L,
            companyName = "Test Client 2",
            location = "Roma",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            socketPrefix = "ROM",
            nextIdNumber = 6  // Dopo 5 test, nextIdNumber dovrebbe essere 6
        )

        every { mockRepository.currentProbe } returns flowOf(null)
        every { mockUserPreferencesRepository.idNumberingStrategy } returns flowOf(IdNumberingStrategy.CONTINUOUS_INCREMENT)

        // Act
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )

        viewModel.selectedClient.value = testClient

        // Assert
        viewModel.socketName.test {
            val socketName = awaitItem()
            assertEquals("ROM006", socketName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 6: Continuous Increment Strategy - Deleted Report Doesn't Affect Numbering
     * Verifica che con CONTINUOUS_INCREMENT, eliminare il report #3 non influenzi la numerazione
     * (il prossimo ID sarà 6, non 3)
     */
    @Test
    fun `test continuous increment strategy - deleted report does not affect numbering`() = runTest {
        // Arrange
        val testClient = Client(
            clientId = 3L,
            companyName = "Test Client 3",
            location = "Torino",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            socketPrefix = "TOR",
            nextIdNumber = 6  // nextIdNumber è 6 (dopo 5 test creati)
        )

        // Report esistenti: 1, 2, 4, 5 (il #3 è stato eliminato)
        val existingReports = listOf(
            Report(reportId = 1, clientId = 3L, timestamp = 1000, socketName = "TOR001", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 2, clientId = 3L, timestamp = 2000, socketName = "TOR002", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 4, clientId = 3L, timestamp = 4000, socketName = "TOR004", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 5, clientId = 3L, timestamp = 5000, socketName = "TOR005", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}")
        )

        every { mockRepository.currentProbe } returns flowOf(null)
        every { mockUserPreferencesRepository.idNumberingStrategy } returns flowOf(IdNumberingStrategy.CONTINUOUS_INCREMENT)
        every { mockReportDao.getReportsForClient(3L) } returns flowOf(existingReports)

        // Act
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )

        viewModel.selectedClient.value = testClient

        // Assert - Con CONTINUOUS_INCREMENT, il prossimo ID è 6 (non 3)
        viewModel.socketName.test {
            val socketName = awaitItem()
            assertEquals("TOR006", socketName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 7: Fill Gaps Strategy - First Test
     * Verifica che con strategia FILL_GAPS, il primo test abbia ID = 1
     */
    @Test
    fun `test fill gaps strategy - first test should have ID 1`() = runTest {
        // Arrange
        val testClient = Client(
            clientId = 4L,
            companyName = "Test Client 4",
            location = "Napoli",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            socketPrefix = "NAP",
            nextIdNumber = 1
        )

        every { mockRepository.currentProbe } returns flowOf(null)
        every { mockUserPreferencesRepository.idNumberingStrategy } returns flowOf(IdNumberingStrategy.FILL_GAPS)
        every { mockReportDao.getReportsForClient(4L) } returns flowOf(emptyList())

        // Act
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )

        viewModel.selectedClient.value = testClient

        // Assert
        viewModel.socketName.test {
            val socketName = awaitItem()
            assertEquals("NAP001", socketName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 8: Fill Gaps Strategy - Fills Deleted Report Gap
     * Verifica che con FILL_GAPS, eliminare il report #3 faccia riutilizzare l'ID 3
     */
    @Test
    fun `test fill gaps strategy - reuses deleted report ID`() = runTest {
        // Arrange
        val testClient = Client(
            clientId = 5L,
            companyName = "Test Client 5",
            location = "Palermo",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            socketPrefix = "PAL",
            nextIdNumber = 6
        )

        // Report esistenti: 1, 2, 4, 5 (il #3 è stato eliminato)
        val existingReports = listOf(
            Report(reportId = 1, clientId = 5L, timestamp = 1000, socketName = "PAL001", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 2, clientId = 5L, timestamp = 2000, socketName = "PAL002", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 4, clientId = 5L, timestamp = 4000, socketName = "PAL004", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 5, clientId = 5L, timestamp = 5000, socketName = "PAL005", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}")
        )

        every { mockRepository.currentProbe } returns flowOf(null)
        every { mockUserPreferencesRepository.idNumberingStrategy } returns flowOf(IdNumberingStrategy.FILL_GAPS)
        every { mockReportDao.getReportsForClient(5L) } returns flowOf(existingReports)

        // Act
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )

        viewModel.selectedClient.value = testClient

        // Assert - Con FILL_GAPS, il prossimo ID è 3 (riempie il gap)
        viewModel.socketName.test {
            val socketName = awaitItem()
            assertEquals("PAL003", socketName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 9: Fill Gaps Strategy - Multiple Gaps, Uses First
     * Verifica che con FILL_GAPS e più gap, venga usato il primo gap disponibile
     */
    @Test
    fun `test fill gaps strategy - with multiple gaps uses first gap`() = runTest {
        // Arrange
        val testClient = Client(
            clientId = 6L,
            companyName = "Test Client 6",
            location = "Firenze",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            socketPrefix = "FIR",
            nextIdNumber = 8
        )

        // Report esistenti: 1, 4, 5, 7 (gap: 2, 3, 6)
        val existingReports = listOf(
            Report(reportId = 1, clientId = 6L, timestamp = 1000, socketName = "FIR001", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 4, clientId = 6L, timestamp = 4000, socketName = "FIR004", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 5, clientId = 6L, timestamp = 5000, socketName = "FIR005", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 7, clientId = 6L, timestamp = 7000, socketName = "FIR007", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}")
        )

        every { mockRepository.currentProbe } returns flowOf(null)
        every { mockUserPreferencesRepository.idNumberingStrategy } returns flowOf(IdNumberingStrategy.FILL_GAPS)
        every { mockReportDao.getReportsForClient(6L) } returns flowOf(existingReports)

        // Act
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )

        viewModel.selectedClient.value = testClient

        // Assert - Con FILL_GAPS, il prossimo ID è 2 (primo gap)
        viewModel.socketName.test {
            val socketName = awaitItem()
            assertEquals("FIR002", socketName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 10: Fill Gaps Strategy - No Gaps, Uses Next Number
     * Verifica che con FILL_GAPS senza gap, venga usato il prossimo numero dopo l'ultimo
     */
    @Test
    fun `test fill gaps strategy - no gaps uses next number after last`() = runTest {
        // Arrange
        val testClient = Client(
            clientId = 7L,
            companyName = "Test Client 7",
            location = "Bologna",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            socketPrefix = "BOL",
            nextIdNumber = 6
        )

        // Report esistenti: 1, 2, 3, 4, 5 (nessun gap)
        val existingReports = listOf(
            Report(reportId = 1, clientId = 7L, timestamp = 1000, socketName = "BOL001", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 2, clientId = 7L, timestamp = 2000, socketName = "BOL002", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 3, clientId = 7L, timestamp = 3000, socketName = "BOL003", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 4, clientId = 7L, timestamp = 4000, socketName = "BOL004", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}"),
            Report(reportId = 5, clientId = 7L, timestamp = 5000, socketName = "BOL005", notes = null, probeName = "Sonda", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}")
        )

        every { mockRepository.currentProbe } returns flowOf(null)
        every { mockUserPreferencesRepository.idNumberingStrategy } returns flowOf(IdNumberingStrategy.FILL_GAPS)
        every { mockReportDao.getReportsForClient(7L) } returns flowOf(existingReports)

        // Act
        viewModel = DashboardViewModel(
            clientDao = mockClientDao,
            testProfileDao = mockTestProfileDao,
            reportDao = mockReportDao,
            repository = mockRepository,
            userPreferencesRepository = mockUserPreferencesRepository
        )

        viewModel.selectedClient.value = testClient

        // Assert - Con FILL_GAPS senza gap, il prossimo ID è 6
        viewModel.socketName.test {
            val socketName = awaitItem()
            assertEquals("BOL006", socketName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}


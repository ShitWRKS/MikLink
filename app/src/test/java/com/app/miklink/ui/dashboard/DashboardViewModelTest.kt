package com.app.miklink.ui.dashboard

import app.cash.turbine.test
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.repository.AppRepository
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
        }

        // Mock del Repository (sarà configurato per ogni test)
        mockRepository = mockk(relaxed = true)
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
            name = "Test Probe",
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
            repository = mockRepository
        )


        // Assert
        viewModel.currentProbe.test {
            val probeValue = awaitItem()
            assertEquals(onlineProbe, probeValue)
            assertEquals(1L, probeValue?.probeId)
            assertEquals("Test Probe", probeValue?.name)
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
            name = "Offline Probe",
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
            repository = mockRepository
        )


        // Assert
        viewModel.currentProbe.test {
            val probeValue = awaitItem()
            assertEquals(offlineProbe, probeValue)
            assertEquals(2L, probeValue?.probeId)
            assertEquals("Offline Probe", probeValue?.name)
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
            repository = mockRepository
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
}


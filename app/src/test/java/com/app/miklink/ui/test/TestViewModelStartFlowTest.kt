package com.app.miklink.ui.test

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.db.model.TestProfile
import com.app.miklink.data.network.NeighborDetail
import com.app.miklink.data.network.PingResult
import com.app.miklink.data.network.MonitorResponse
import com.app.miklink.data.repository.AppRepository
import com.app.miklink.data.repository.AppRepository.NetworkConfigFeedback
import com.app.miklink.utils.UiState
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.lang.reflect.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModelStartFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var clientDao: ClientDao
    private lateinit var probeDao: ProbeConfigDao
    private lateinit var profileDao: TestProfileDao
    private lateinit var reportDao: ReportDao
    private lateinit var repository: AppRepository
    private lateinit var moshi: Moshi

    private lateinit var viewModel: TestViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle()
        clientDao = mockk(relaxed = true)
        probeDao = mockk(relaxed = true)
        profileDao = mockk(relaxed = true)
        reportDao = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        // Mock Moshi per evitare problemi di adapter Any
        moshi = mockk(relaxed = true)
        val fakeMapAdapter = mockk<JsonAdapter<Map<String, Any>>>(relaxed = true) {
            every { toJson(any()) } returns "{}"
        }
        every { moshi.adapter<Map<String, Any>>(any()) } returns fakeMapAdapter
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildHappyPathFixtures(pingCount: Int = 10): Triple<Client, ProbeConfig, TestProfile> {
        val client = Client(
            clientId = 1L,
            companyName = "ACME Corp",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "1G",
            socketPrefix = "AC",
            nextIdNumber = 1,
            lastFloor = null,
            lastRoom = null
        )
        val probe = ProbeConfig(
            probeId = 2L,
            name = "Probe-01",
            ipAddress = "192.168.88.1",
            username = "admin",
            password = "pass",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )
        val profile = TestProfile(
            profileId = 3L,
            profileName = "Full",
            profileDescription = null,
            runTdr = true,
            runLinkStatus = true,
            runLldp = true,
            runPing = true,
            pingTarget1 = "8.8.8.8",
            pingTarget2 = null,
            pingTarget3 = null,
            pingCount = pingCount
        )
        return Triple(client, probe, profile)
    }

    private fun createViewModelWithSavedState(clientId: Long?, probeId: Long?, profileId: Long?, socketName: String = "AC001") {
        if (clientId != null) savedStateHandle["clientId"] = clientId
        if (probeId != null) savedStateHandle["probeId"] = probeId
        if (profileId != null) savedStateHandle["profileId"] = profileId
        savedStateHandle["socketName"] = socketName
        viewModel = TestViewModel(
            savedStateHandle = savedStateHandle,
            clientDao = clientDao,
            probeDao = probeDao,
            profileDao = profileDao,
            reportDao = reportDao,
            repository = repository,
            moshi = moshi
        )
    }

    // NOTE: I test per il flusso completo di startTest() richiederebbero un refactoring
    // del ViewModel per rendere testabili le singole fasi. Attualmente la complessità
    // dell'orchestrazione (DAO Flow, coroutines, serializzazione Moshi) rende difficile
    // un unit test affidabile. Questi test sono migliori come Integration Test.

    @Test
    fun `startTest with invalid saved state emits Error and doesn't call repository`() = runTest(testDispatcher) {
        // Arrange: no ids in saved state
        createViewModelWithSavedState(clientId = null, probeId = null, profileId = null)

        // Act
        viewModel.startTest()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val final = viewModel.uiState.value
        assertTrue(final is UiState.Error)
        coVerify(exactly = 0) { repository.applyClientNetworkConfig(any(), any(), any()) }
        coVerify(exactly = 0) { repository.getLinkStatus(any(), any()) }
        coVerify(exactly = 0) { repository.getNeighborsForInterface(any(), any()) }
        coVerify(exactly = 0) { repository.runPing(any(), any(), any(), any()) }
    }
}

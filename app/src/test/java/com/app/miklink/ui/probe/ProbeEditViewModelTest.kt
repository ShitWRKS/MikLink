package com.app.miklink.ui.probe

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.repository.AppRepository
import com.app.miklink.data.repository.ProbeCheckResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for ProbeEditViewModel
 *
 * Covers:
 * - Initial state loading (new probe and existing probe)
 * - Connection verification (success/error flows)
 * - Probe saving logic
 * - State management for UI fields
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProbeEditViewModelTest {

    // Mocks
    private lateinit var probeConfigDao: ProbeConfigDao
    private lateinit var appRepository: AppRepository
    private lateinit var savedStateHandle: SavedStateHandle

    // System Under Test
    private lateinit var viewModel: ProbeEditViewModel

    // Test Dispatcher
    private val testDispatcher = UnconfinedTestDispatcher()

    // Test Data
    private val mockProbe = ProbeConfig(
        probeId = 1L,
        ipAddress = "192.168.88.1",
        
        username = "admin",
        password = "password123",
        isHttps = false,
        testInterface = "ether1",
        isOnline = true,
        modelName = "RouterBOARD 3011",
        tdrSupported = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        probeConfigDao = mockk(relaxed = true)
        appRepository = mockk(relaxed = true)
        savedStateHandle = mockk(relaxed = true)

        // Default mock behavior: no probe loaded (new probe mode)
        every { savedStateHandle.get<Long>("probeId") } returns -1L
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================
    // TEST 1: Load Single Probe (Init)
    // ============================================

    @Test
    fun `GIVEN existing probe WHEN ViewModel created THEN state fields are populated with probe data`() = runTest {
        // Given: DAO returns an existing probe
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(mockProbe)

        // When: ViewModel is created
        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)

        // Then: State fields should match the probe data (name removed from model)
        assertEquals("192.168.88.1", viewModel.ipAddress.value)
        assertEquals("admin", viewModel.username.value)
        assertEquals("password123", viewModel.password.value)
        assertFalse(viewModel.isHttps.value)
        assertEquals("ether1", viewModel.testInterface.value)
    }

    @Test
    fun `GIVEN existing probe with modelName WHEN ViewModel created THEN verificationState is Success`() = runTest {
        // Given: DAO returns a probe that has been verified (has modelName)
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(mockProbe)

        // When: ViewModel is created
        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)

        // Then: Verification state should be Success
        val state = viewModel.verificationState.value
        assertTrue(state is VerificationState.Success)
        assertEquals("RouterBOARD 3011", (state as VerificationState.Success).boardName)
        assertEquals(listOf("ether1"), state.interfaces)
    }



    // ============================================
    // TEST 2: onSaveClicked (No Validation - Direct Save)
    // ============================================

    @Test
    fun `GIVEN valid probe data WHEN onSaveClicked called THEN probe is saved to DAO`() = runTest {
        // Given: ViewModel with valid data
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(null)
        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)

        // name editing is no longer allowed; onSaveClicked will set the name to a generic value
        viewModel.ipAddress.value = "10.0.0.1"
        viewModel.username.value = "testuser"
        viewModel.password.value = "testpass"
        viewModel.isHttps.value = true
        viewModel.testInterface.value = "ether2"

        // When: onSaveClicked is called
        viewModel.onSaveClicked()

        // Then: upsertSingle should be called with correct probe data
        coVerify {
            probeConfigDao.upsertSingle(
                match {
                    it.probeId == 1L &&
                    it.ipAddress == "10.0.0.1" &&
                    it.username == "testuser" &&
                    it.password == "testpass" &&
                    it.isHttps &&
                    it.testInterface == "ether2"
                }
            )
        }

        // And: isSaved state should be true
        assertTrue(viewModel.isSaved.value)
    }

    @Test
    fun `GIVEN editing mode WHEN onSaveClicked called THEN probe is saved with correct ID`() = runTest {
        // Given: ViewModel in editing mode (probeId = 5)
        every { savedStateHandle.get<Long>("probeId") } returns 5L
        coEvery { probeConfigDao.getProbeById(5L) } returns flowOf(mockProbe.copy(probeId = 5L))

        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)
        // editing name is not allowed; probe's name will become the generic 'Sonda'

        // When: onSaveClicked is called
        viewModel.onSaveClicked()

        // Then: upsertSingle should be called with probeId = 5
        coVerify {
            probeConfigDao.upsertSingle(
                match { it.probeId == 5L }
            )
        }
    }

    // ============================================
    // TEST 3: checkProbeConnection (Success)
    // ============================================

    @Test
    fun `GIVEN valid connection details WHEN onVerifyClicked called THEN verificationState emits Success`() = runTest {
        // Given: ViewModel with connection details
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(null)
        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)

        viewModel.ipAddress.value = "192.168.1.1"
        viewModel.username.value = "admin"
        viewModel.password.value = "pass"

        val successResult = ProbeCheckResult.Success(
            boardName = "CCR1036-12G-4S",
            interfaces = listOf("ether1", "ether2", "ether3")
        )
        coEvery { appRepository.checkProbeConnection(any()) } returns successResult

        // When: onVerifyClicked is called
        viewModel.verificationState.test {
            // Skip initial Idle state
            assertEquals(VerificationState.Idle, awaitItem())

            viewModel.onVerifyClicked()

            // Then: State should transition to Loading
            assertEquals(VerificationState.Loading, awaitItem())

            // Then: State should transition to Success
            val successState = awaitItem() as VerificationState.Success
            assertEquals("CCR1036-12G-4S", successState.boardName)
            assertEquals(listOf("ether1", "ether2", "ether3"), successState.interfaces)

            // And: testInterface should be populated with first interface
            assertEquals("ether1", viewModel.testInterface.value)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN successful verification WHEN onVerifyClicked called THEN probe metadata is updated`() = runTest {
        // Given: ViewModel setup
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(null)
        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)

        viewModel.ipAddress.value = "192.168.1.1"

        val successResult = ProbeCheckResult.Success(
            boardName = "RB4011iGS+",
            interfaces = listOf("sfp-sfpplus1")
        )
        coEvery { appRepository.checkProbeConnection(any()) } returns successResult

        // When: onVerifyClicked is called
        viewModel.onVerifyClicked()

        // Then: testInterface should be set to first interface
        assertEquals("sfp-sfpplus1", viewModel.testInterface.value)
    }

    // ============================================
    // TEST 4: checkProbeConnection (Error)
    // ============================================

    @Test
    fun `GIVEN connection failure WHEN onVerifyClicked called THEN verificationState emits Error`() = runTest {
        // Given: ViewModel with connection details
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(null)
        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)

        viewModel.ipAddress.value = "192.168.1.1"

        val errorResult = ProbeCheckResult.Error("Timeout: Unable to connect")
        coEvery { appRepository.checkProbeConnection(any()) } returns errorResult

        // When: onVerifyClicked is called
        viewModel.verificationState.test {
            // Skip initial Idle state
            assertEquals(VerificationState.Idle, awaitItem())

            viewModel.onVerifyClicked()

            // Then: State should transition to Loading
            assertEquals(VerificationState.Loading, awaitItem())

            // Then: State should transition to Error
            val errorState = awaitItem() as VerificationState.Error
            assertEquals("Timeout: Unable to connect", errorState.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN authentication error WHEN onVerifyClicked called THEN verificationState contains error message`() = runTest {
        // Given: ViewModel setup
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(null)
        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)

        viewModel.ipAddress.value = "192.168.1.1"

        val errorResult = ProbeCheckResult.Error("401 Unauthorized")
        coEvery { appRepository.checkProbeConnection(any()) } returns errorResult

        // When: onVerifyClicked is called
        viewModel.onVerifyClicked()

        // Then: verificationState should contain the error
        val state = viewModel.verificationState.value as VerificationState.Error
        assertEquals("401 Unauthorized", state.message)
    }

    // ============================================
    // TEST 5: State Reset on Connection Details Change
    // ============================================

    @Test
    fun `GIVEN verified probe WHEN connection details change THEN verificationState is reset to Error`() = runTest {
        // Given: ViewModel with a successfully verified probe
        coEvery { probeConfigDao.getSingleProbe() } returns flowOf(null)
        viewModel = ProbeEditViewModel(probeConfigDao, appRepository, savedStateHandle)

        viewModel.ipAddress.value = "192.168.1.1"
        viewModel.username.value = "admin"
        viewModel.password.value = "password"

        val successResult = ProbeCheckResult.Success("RB750", listOf("ether1"))
        coEvery { appRepository.checkProbeConnection(any()) } returns successResult

        viewModel.onVerifyClicked()

        // Wait for verification to complete
        testScheduler.advanceUntilIdle()

        // Verify that state is Success
        val successState = viewModel.verificationState.value as VerificationState.Success
        assertEquals("RB750", successState.boardName)

        // When: Connection details change (IP address)
        viewModel.ipAddress.value = "192.168.1.2"

        // Allow the flow to process the change
        testScheduler.advanceUntilIdle()

        // Then: Verification state should reset to Error
        val resetState = viewModel.verificationState.value as VerificationState.Error
        assertTrue(resetState.message.contains("Probe details changed"))
    }
}


/*
 * Purpose: Verify ProbeEditViewModel persists probe metadata (board/model name) after a successful verification flow.
 * Inputs: Fake ProbeRepository and ProbeConnectivityRepository returning a known boardName.
 * Outputs: Assertions that saved ProbeConfig carries the resolved boardName, preventing regressions that return "Unknown Board".
 * Notes: Uses MainDispatcherRule to drive viewModelScope coroutines in tests.
 */
package com.app.miklink.ui.probe

import androidx.lifecycle.SavedStateHandle
import com.app.miklink.core.data.repository.ProbeCheckResult
import com.app.miklink.core.data.repository.probe.ProbeConnectivityRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.testsupport.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProbeEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeProbeRepository = FakeProbeRepository()
    private val fakeConnectivityRepository = FakeProbeConnectivityRepository(
        boardName = "hAP ax^2",
        interfaces = listOf("ether1", "ether2")
    )

    @Test
    fun `verify success persists board name when saving`() = runTest {
        val viewModel = ProbeEditViewModel(
            probeRepository = fakeProbeRepository,
            probeConnectivityRepository = fakeConnectivityRepository,
            savedStateHandle = SavedStateHandle()
        )

        viewModel.ipAddress.value = "172.29.0.1"
        viewModel.username.value = "admin"
        viewModel.password.value = "pass"

        viewModel.onVerifyClicked()
        advanceUntilIdle()
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val saved = fakeProbeRepository.lastSaved
        assertNotNull(saved)
        assertEquals("hAP ax^2", saved?.modelName)
        // Also ensure we keep the interface chosen by verify flow
        assertEquals("ether1", saved?.testInterface)
        assertTrue(saved?.tdrSupported == true)

        // Recreate ViewModel (simulate reopening screen) and ensure persisted board is loaded
        val reloaded = ProbeEditViewModel(
            probeRepository = fakeProbeRepository,
            probeConnectivityRepository = fakeConnectivityRepository,
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()

        val loadedState = reloaded.verificationState.value
        assertTrue(loadedState is VerificationState.Success)
        assertEquals("hAP ax^2", (loadedState as VerificationState.Success).boardName)
        assertEquals("ether1", reloaded.testInterface.value)
    }

    @Test
    fun `verify fallback toggles https off and surfaces warning`() = runTest {
        val viewModel = ProbeEditViewModel(
            probeRepository = fakeProbeRepository,
            probeConnectivityRepository = FakeProbeConnectivityRepository(
                boardName = "RB5009",
                interfaces = listOf("ether1"),
                didFallbackToHttp = true,
                warning = "fallback-http"
            ),
            savedStateHandle = SavedStateHandle()
        )

        viewModel.ipAddress.value = "10.0.0.1"
        viewModel.username.value = "admin"
        viewModel.password.value = "pass"
        viewModel.isHttps.value = true

        viewModel.onVerifyClicked()
        advanceUntilIdle()

        assertTrue(viewModel.verificationState.value is VerificationState.Success)
        val state = viewModel.verificationState.value as VerificationState.Success
        assertTrue(state.didFallbackToHttp)
        assertEquals("fallback-http", state.warning)
        assertEquals(false, viewModel.isHttps.value)
    }

    private class FakeProbeRepository : ProbeRepository {
        private val state = MutableStateFlow<ProbeConfig?>(null)
        var lastSaved: ProbeConfig? = null

        override fun observeProbeConfig(): Flow<ProbeConfig?> = state

        override suspend fun getProbeConfig(): ProbeConfig? = state.first()

        override suspend fun saveProbeConfig(config: ProbeConfig) {
            lastSaved = config
            state.value = config
        }
    }

    private class FakeProbeConnectivityRepository(
        private val boardName: String,
        private val interfaces: List<String>,
        private val didFallbackToHttp: Boolean = false,
        private val warning: String? = null
    ) : ProbeConnectivityRepository {
        override suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult {
            return ProbeCheckResult.Success(
                boardName = boardName,
                interfaces = interfaces,
                effectiveIsHttps = probe.isHttps && !didFallbackToHttp,
                didFallbackToHttp = didFallbackToHttp,
                warning = warning
            )
        }
    }
}

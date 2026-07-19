/*
 * Purpose: Verify that all test steps preserve typed TestError via TestExecutionException.
 * Inputs: TestExecutionException with various TestError subtypes injected into mock repositories.
 * Outputs: StepResult.Failed with the same TestError type (not Unexpected).
 * Notes: Covers mandatory test matrix B — each step preserves TestError type.
 */
package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.data.repository.test.NetworkConfigRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.model.TestExecutionException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTestErrorPreservationTest {

    private fun defaultContext(): TestExecutionContext {
        val client = mockk<Client>(relaxed = true)
        val probe = mockk<ProbeConfig>(relaxed = true) {
            every { testInterface } returns "ether1"
        }
        val profile = mockk<TestProfile>(relaxed = true)
        return TestExecutionContext(
            client = client,
            probeConfig = probe,
            testProfile = profile,
            socketId = "S1",
            notes = null
        )
    }

    // --- Helper to verify StepResult.Failed preserves the exact TestError type ---

    private fun assertPreserved(result: StepResult<*>, expectedError: TestError) {
        assertTrue("Expected Failed but got $result", result is StepResult.Failed)
        val failed = result as StepResult.Failed
        assertEquals(
            "Expected ${expectedError::class.simpleName} but got ${failed.error::class.simpleName}",
            expectedError::class, failed.error::class
        )
        assertEquals(expectedError.message, failed.error.message)
    }

    private val probeUnavailable = TestError.ProbeUnavailable("probe lost")
    private val authError = TestError.Authentication("auth failed")
    private val invalidResponse = TestError.InvalidResponse("bad response")
    private val routerOsError = TestError.RouterOsError("router error", code = 500, detail = null)
    private val timeout = TestError.Timeout("timed out")
    private val unexpected = TestError.Unexpected("unexpected")

    private val testErrors = listOf(
        probeUnavailable, authError, invalidResponse, routerOsError, timeout, unexpected
    )

    // --- LinkStatusStep ---

    @Test
    fun `LinkStatusStep preserves all TestError types`() = runTest {
        for (error in testErrors) {
            val repo = mockk<MikroTikTestRepository>()
            coEvery { repo.monitorEthernet(any(), any(), any()) } throws TestExecutionException(error)
            val step = LinkStatusStepImpl(repo)
            assertPreserved(step.run(defaultContext()), error)
        }
    }

    // --- CableTestStep ---

    @Test
    fun `CableTestStep preserves all TestError types`() = runTest {
        for (error in testErrors) {
            val repo = mockk<MikroTikTestRepository>()
            coEvery { repo.cableTest(any(), any(), any()) } throws TestExecutionException(error)
            val step = CableTestStepImpl(repo)
            assertPreserved(step.run(defaultContext()), error)
        }
    }

    // --- NetworkConfigStep ---

    @Test
    fun `NetworkConfigStep preserves all TestError types`() = runTest {
        for (error in testErrors) {
            val repo = mockk<NetworkConfigRepository>()
            coEvery { repo.applyClientNetworkConfig(any(), any(), any()) } throws TestExecutionException(error)
            val step = NetworkConfigStepImpl(repo)
            assertPreserved(step.run(defaultContext()), error)
        }
    }

    // --- NeighborDiscoveryStep ---

    @Test
    fun `NeighborDiscoveryStep preserves all TestError types`() = runTest {
        for (error in testErrors) {
            val repo = mockk<MikroTikTestRepository>()
            coEvery { repo.neighbors(any(), any()) } throws TestExecutionException(error)
            val prefs = mockk<UserPreferencesRepository>()
            every { prefs.neighborDiscoveryProtocols } returns flowOf(setOf("LLDP"))
            val step = NeighborDiscoveryStepImpl(repo, prefs)
            assertPreserved(step.run(defaultContext()), error)
        }
    }

    // --- PingStep ---

    @Test
    fun `PingStep preserves ProbeUnavailable via repository`() = runTest {
        val repo = mockk<MikroTikTestRepository>()
        coEvery { repo.ping(any(), any(), any(), any()) } throws TestExecutionException(probeUnavailable)
        val step = PingStepImpl(repo, mockk(relaxed = true) {
            coEvery { resolve(any(), any(), any(), any()) } returns "8.8.8.8"
        })
        val profile = mockk<TestProfile>(relaxed = true) {
            every { pingTarget1 } returns "8.8.8.8"
            every { pingTarget2 } returns null
            every { pingTarget3 } returns null
            every { pingCount } returns 4
        }
        val ctx = defaultContext().let {
            it.copy(testProfile = profile)
        }
        assertPreserved(step.run(ctx), probeUnavailable)
    }

    // --- SpeedTestStep ---

    @Test
    fun `SpeedTestStep preserves all TestError types`() = runTest {
        for (error in testErrors) {
            val repo = mockk<MikroTikTestRepository>()
            coEvery { repo.speedTest(any(), any(), any(), any(), any()) } throws TestExecutionException(error)
            val step = SpeedTestStepImpl(repo)
            val profile = mockk<TestProfile>(relaxed = true)
            val client = mockk<Client>(relaxed = true) {
                every { speedTestServerAddress } returns "speed.example.com"
            }
            val probe = mockk<ProbeConfig>(relaxed = true)
            val ctx = TestExecutionContext(client, probe, profile, "S1", null)
            assertPreserved(step.run(ctx), error)
        }
    }
}

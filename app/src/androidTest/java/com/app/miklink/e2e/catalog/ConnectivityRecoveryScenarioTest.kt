package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.e2e.support.CleanupStatus
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.e2e.support.SessionPolicy
import com.app.miklink.e2e.support.TerminalStatus
import com.app.miklink.e2e.support.TestFixtureManager
import com.app.miklink.e2e.support.WifiDisruptionController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectivityRecoveryScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("connectivity-recovery")

    @Test
    fun probeLossTerminatesRestoresAndRecoversOnlyWithExplicitAuthority() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val policy = SessionPolicy(
            allowWifiDisruption = arguments.getString("allowWifiDisruption").toBoolean(),
            hostControlRetained = arguments.getString("hostControlRetained").toBoolean()
        )
        val controller = WifiDisruptionController()
        if (!policy.allowWifiDisruption || !policy.hostControlRetained) {
            val refused = controller.execute(policy) { error("unauthorized action executed") }
            assertEquals(TerminalStatus.NOT_RUN, refused.status)
            assertFalse(refused.actionExecuted)
            scenarioRule.notRun(refused.reasonCode, "wifi-disruption-authorized")
        }

        val deps = appOnlyDependencies()
        val probe = deps.probeRepository().getProbeConfig()
        if (probe == null || probe.ipAddress.isBlank() || probe.username.isBlank() || probe.testInterface.isBlank()) {
            scenarioRule.notRun("PROBE_NOT_CONFIGURED", "configured-probe")
        }
        val fixtures = TestFixtureManager(
            sessionId = "connectivity-recovery-${System.nanoTime()}",
            clients = deps.clientRepository(),
            profiles = deps.testProfileRepository(),
            reports = deps.reportRepository()
        )
        val core = fixtures.createCoreFixtures()
        try {
            val context = TestExecutionContext(
                client = core.client,
                probeConfig = probe,
                testProfile = core.profile.copy(runLinkStatus = true),
                socketId = "RECOVERY-001",
                notes = "session-owned"
            )
            if (deps.linkStatusStep().run(context) !is StepResult.Success<*>) {
                scenarioRule.notRun("PROBE_NOT_REACHABLE_BEFORE_DISRUPTION", "reachable-probe")
            }

            val disrupted = controller.execute(policy) {
                val duringLoss = deps.linkStatusStep().run(context)
                check(duringLoss is StepResult.Failed<*>) {
                    "PROBE_OPERATION_DID_NOT_FAIL_DURING_WIFI_LOSS"
                }
            }
            assertEquals("Disruption must have a bounded terminal result", TerminalStatus.PASS, disrupted.status)
            assertEquals(CleanupStatus.PASS, disrupted.cleanup.status)
            assertTrue(disrupted.actionExecuted)

            val recovered = deps.linkStatusStep().run(context)
            assertTrue("Probe path did not recover after Wi-Fi restoration", recovered is StepResult.Success<*>)
        } finally {
            val cleanup = fixtures.cleanup()
            scenarioRule.recordCleanup(cleanup)
            assertEquals(CleanupStatus.PASS, cleanup.status)
        }
    }

}

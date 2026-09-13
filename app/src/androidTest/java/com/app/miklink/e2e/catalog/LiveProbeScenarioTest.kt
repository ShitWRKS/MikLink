package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.domain.model.TdrCapability
import com.app.miklink.core.domain.test.logging.DebugTraceCorrelation
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.e2e.support.CleanupStatus
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.e2e.support.TestFixtureManager
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveProbeScenarioTest {
    @get:Rule
    val scenarioRule = ScenarioRule.catalog { description ->
        when (description.methodName) {
            "linkCase" -> "live-link"
            "tdrCase" -> "live-tdr"
            "networkCase" -> "live-network"
            "neighborsCase" -> "live-neighbors"
            "pingCase" -> "live-ping"
            "speedCase" -> "live-speed"
            else -> "live-probe"
        }
    }

    @Test fun linkCase() = runCase("live-link") { deps, context -> deps.linkStatusStep().run(context) }

    @Test fun tdrCase() = runCase("live-tdr", skipWhen = { context ->
        context.probeConfig.tdrCapability == TdrCapability.UNSUPPORTED
    }) { deps, context -> deps.cableTestStep().run(context) }

    @Test fun networkCase() = runCase("live-network") { deps, context -> deps.networkConfigStep().run(context) }

    @Test fun neighborsCase() = runCase("live-neighbors") { deps, context -> deps.neighborDiscoveryStep().run(context) }

    @Test fun pingCase() = runCase("live-ping") { deps, context -> deps.pingStep().run(context) }

    @Test fun speedCase() = runCase("live-speed", requireSpeedServer = true) { deps, context ->
        deps.speedTestStep().run(context)
    }

    private fun runCase(
        scenarioId: String,
        requireSpeedServer: Boolean = false,
        skipWhen: (TestExecutionContext) -> Boolean = { false },
        execute: suspend (com.app.miklink.e2e.DebugE2EEntryPoint, TestExecutionContext) -> StepResult<*>
    ) = runBlocking {
        val deps = appOnlyDependencies()
        val probe = deps.probeRepository().getProbeConfig()
        if (probe == null || probe.ipAddress.isBlank() || probe.username.isBlank() || probe.testInterface.isBlank()) {
            scenarioRule.notRun("PROBE_NOT_CONFIGURED", "configured-probe")
        }

        val manager = TestFixtureManager(
            sessionId = "$scenarioId-${System.nanoTime()}",
            clients = deps.clientRepository(),
            profiles = deps.testProfileRepository(),
            reports = deps.reportRepository()
        )
        val fixtures = manager.createCoreFixtures()
        try {
            val configuredSpeed = deps.clientRepository().observeAllClients().first()
                .firstOrNull { !it.speedTestServerAddress.isNullOrBlank() && it.clientId != fixtures.client.clientId }
            val client = if (configuredSpeed != null) {
                fixtures.client.copy(
                    speedTestServerAddress = configuredSpeed.speedTestServerAddress,
                    speedTestServerUser = configuredSpeed.speedTestServerUser,
                    speedTestServerPassword = configuredSpeed.speedTestServerPassword
                ).also { deps.clientRepository().updateClient(it) }
            } else fixtures.client
            if (requireSpeedServer && client.speedTestServerAddress.isNullOrBlank()) {
                scenarioRule.notRun("SPEED_SERVER_NOT_CONFIGURED", "speed-server")
            }

            val profile = fixtures.profile.copy(
                runTdr = scenarioId == "live-tdr",
                runLinkStatus = scenarioId == "live-link",
                runLldp = scenarioId == "live-neighbors",
                runPing = scenarioId == "live-ping",
                pingTarget1 = if (scenarioId == "live-ping") "DHCP_GATEWAY" else null,
                runSpeedTest = scenarioId == "live-speed"
            )
            val context = TestExecutionContext(client, probe, profile, "E2E-001", "session-owned")
            if (skipWhen(context)) {
                scenarioRule.skip("CAPABILITY_NOT_APPLICABLE", "tdr-capability")
            }

            val correlation = DebugTraceCorrelation(
                runId = UUID.randomUUID().toString(),
                sessionId = "device-live-session",
                scenarioId = scenarioId
            )
            deps.debugTraceRunContext().set(correlation)
            val result = try {
                execute(deps, context)
            } finally {
                deps.debugTraceRunContext().clear(correlation.runId)
            }
            when (result) {
                is StepResult.Success<*> -> Unit
                is StepResult.Skipped -> scenarioRule.skip(result.reason, "step-capability")
                is StepResult.Failed<*> -> throw AssertionError("$scenarioId failed: ${result.error}")
            }
        } finally {
            val cleanup = manager.cleanup()
            scenarioRule.recordCleanup(cleanup)
            check(cleanup.status == CleanupStatus.PASS) { cleanup.reasonCode ?: "CLEANUP_FAILED" }
        }
    }

}

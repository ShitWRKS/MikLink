package com.app.miklink.e2e

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.app.miklink.core.domain.model.TdrCapability
import com.app.miklink.core.domain.test.logging.DebugTraceCorrelation
import com.app.miklink.R
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentUiTags
import com.app.miklink.ui.test.components.TestExecutionTags
import dagger.hilt.android.EntryPointAccessors
import com.app.miklink.e2e.support.ArtifactSecretScanner
import com.app.miklink.e2e.support.CleanupStatus
import com.app.miklink.e2e.support.CoreFixtures
import com.app.miklink.e2e.support.RedactionStatus
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.e2e.support.TestFixtureManager
import com.app.miklink.e2e.support.dismissKeyguardIfPossible
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class LiveProbeE2ETest {
    private var fixtureManager: TestFixtureManager? = null
    private lateinit var fixtures: CoreFixtures
    private var savedSessionReport: TestReport? = null
    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private val scenarioRule = ScenarioRule.catalog(
        scenarioId = "legacy-live-probe",
        timeoutMs = 240_000L,
        cleanup = { cleanupLiveFixtures() }
    )

    @get:Rule
    val ruleChain: TestRule = RuleChain
        .outerRule(scenarioRule)
        .around(preflightRule())

    @Test
    fun runLiveProbeE2E() {
        logE2e(
            event = "MIKLINK_E2E_START",
            fields = mapOf(
                "test" to "LiveProbeE2ETest",
                "mode" to "physical_device_live_probe"
            )
        )

        var requestedTraceRunId: String? = null
        try {
            device.executeShellCommand("am start -W -n com.app.miklink/.MainActivity")
            waitForTag(DashboardTags.CLIENT_SELECTOR, 20_000L, "dashboard_not_ready", notRunOnTimeout = false)
            logE2e("MIKLINK_E2E_STEP", mapOf("step" to "app_started"))

            ensureLiveFixtures()
            val reportIdsBefore = runBlocking {
                dependencies().reportRepository().observeAllReports().first().map { it.reportId }.toSet()
            }

            val selectedClientId = selectClient()
            val selectedProfileId = selectProfile()

            val startNode = device.wait(
                Until.findObject(By.res(DashboardTags.START_TEST_BUTTON)),
                10_000L
            ) ?: throw AssertionError("start_button_not_found")
            if (!startNode.isEnabled) {
                emitNotRun("missing_live_probe_or_configuration")
            }

            logE2e(
                event = "MIKLINK_E2E_STEP",
                fields = mapOf(
                    "step" to "selection_completed",
                    "clientId" to selectedClientId,
                    "profileId" to selectedProfileId
                )
            )

            val tracesBefore = traceFiles().map(File::getCanonicalPath).toSet()
            val traceRequestId = "requested-${UUID.randomUUID()}"
            requestedTraceRunId = traceRequestId
            dependencies().debugTraceRunContext().set(
                DebugTraceCorrelation(
                    runId = traceRequestId,
                    sessionId = scenarioRule.sessionId,
                    scenarioId = LEGACY_SCENARIO_ID
                )
            )
            startNode.click()
            logE2e("MIKLINK_E2E_STEP", mapOf("step" to "test_started_from_ui"))

            waitForTag(TestExecutionTags.HERO_RUNNING, 30_000L, "running_state_not_reached", notRunOnTimeout = false)
            waitForTag(TestExecutionTags.HERO_COMPLETED, LIVE_RESULT_TIMEOUT_MS, "final_result_timeout", notRunOnTimeout = false)

            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val statusText = when {
                hasVisibleText(targetContext.getString(R.string.test_execution_completed_hero_pass)) ->
                    targetContext.getString(R.string.test_execution_completed_hero_pass)
                hasVisibleText(targetContext.getString(R.string.test_execution_completed_hero_fail)) ->
                    targetContext.getString(R.string.test_execution_completed_hero_fail)
                else -> "UNKNOWN"
            }
            val summaryText = when {
                hasVisibleText(targetContext.getString(R.string.test_execution_hero_pass_subtitle)) ->
                    targetContext.getString(R.string.test_execution_hero_pass_subtitle)
                hasVisibleText(targetContext.getString(R.string.test_execution_hero_fail_subtitle)) ->
                    targetContext.getString(R.string.test_execution_hero_fail_subtitle)
                else -> ""
            }

            if (statusText == "UNKNOWN") {
                throw AssertionError("Visible final status text not found")
            }

            logE2e(
                event = "MIKLINK_E2E_VISIBLE_RESULT",
                fields = mapOf(
                    "statusText" to statusText,
                    "summaryText" to summaryText
                )
            )

            val runningStillVisible = device.hasObject(By.res(TestExecutionTags.HERO_RUNNING))
            if (runningStillVisible) {
                throw AssertionError("Run appears stuck in running state")
            }

            listOf("network", "link", "neighbors", "ping").forEach { section ->
                waitForScrollableTag(
                    "${TestExecutionTags.SECTION_CARD_PREFIX}_$section",
                    10_000L
                )
            }
            if (fixtures.profile.runTdr) {
                waitForScrollableTag("${TestExecutionTags.SECTION_CARD_PREFIX}_tdr", 10_000L)
            }

            collectAndRegisterTrace(tracesBefore)
            requireObject(TestExecutionTags.BOTTOM_SAVE).click()
            waitForTag(DashboardTags.SCREEN, 15_000L, "dashboard_not_reached_after_save", notRunOnTimeout = false)
            savedSessionReport = awaitSavedSessionReport(reportIdsBefore)

            requireObject(DashboardTags.HISTORY_BUTTON).click()
            waitForTag(AgentUiTags.History.SCREEN, 10_000L, "history_not_reached", notRunOnTimeout = false)
            requireObject(AgentUiTags.History.SEARCH).text = fixtures.client.companyName
            val savedReport = requireNotNull(savedSessionReport)
            requireObject("${AgentUiTags.History.CLIENT_EXPAND_PREFIX}_${fixtures.client.clientId}").click()
            requireObject("${AgentUiTags.History.REPORT_ITEM_PREFIX}_${savedReport.reportId}").click()
            waitForTag(AgentUiTags.Report.SCREEN, 10_000L, "saved_report_detail_not_reached", notRunOnTimeout = false)
            if (!hasVisibleText(savedReport.socketName.orEmpty())) {
                throw AssertionError("Saved report socket is not visible in history detail")
            }

            requireScrollableObject(AgentUiTags.Report.DELETE).click()
            requireObject(AgentUiTags.Report.DELETE_CONFIRM).click()
            waitForTag(AgentUiTags.History.SCREEN, 10_000L, "history_not_reached_after_delete", notRunOnTimeout = false)
            savedSessionReport = null
            logE2e("MIKLINK_E2E_END", mapOf("status" to "PASS"))
        } catch (notRun: NotRunException) {
            logE2e(
                event = "MIKLINK_E2E_END",
                fields = mapOf("status" to "NOT_RUN", "reason" to notRun.reason)
            )
            scenarioRule.notRun(notRun.reason, "live-probe-prerequisite")
        } catch (error: Throwable) {
            logE2e(
                event = "MIKLINK_E2E_END",
                fields = mapOf("status" to "FAIL", "reason" to (error.message ?: "unknown_failure"))
            )
            throw error
        } finally {
            requestedTraceRunId?.let { expectedRunId ->
                runCatching { dependencies().debugTraceRunContext().clear(expectedRunId) }
            }
        }
    }

    private fun preflightRule(): TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val device = androidx.test.uiautomator.UiDevice.getInstance(instrumentation)
                if (!device.dismissKeyguardIfPossible(instrumentation.targetContext)) {
                    abortPreflight("DEVICE_LOCKED", "device-unlocked")
                }
                val probe = runBlocking { dependencies().probeRepository().getProbeConfig() }
                if (probe == null || probe.ipAddress.isBlank() || probe.username.isBlank()) {
                    abortPreflight("PROBE_NOT_CONFIGURED", "configured-probe")
                }
                if (probe.testInterface.isBlank()) {
                    abortPreflight("PROBE_INTERFACE_NOT_SELECTED", "probe-interface")
                }
                base.evaluate()
            }
        }
    }

    private fun abortPreflight(reason: String, prerequisiteId: String): Nothing {
        // Keep the unchanged legacy shell wrappers able to classify an early
        // prerequisite exit before the Compose rule launches the Activity.
        logE2e(
            event = "MIKLINK_E2E_END",
            fields = mapOf("status" to "NOT_RUN", "reason" to reason)
        )
        scenarioRule.notRun(reason, prerequisiteId)
    }

    private fun selectClient(): Long {
        requireObject(DashboardTags.CLIENT_SELECTOR).click()
        return selectFixtureFromSheet(fixtures.client.clientId, fixtures.client.companyName)
    }

    private fun selectProfile(): Long {
        requireObject(DashboardTags.PROFILE_SELECTOR).click()
        return selectFixtureFromSheet(fixtures.profile.profileId, fixtures.profile.profileName)
    }

    private fun ensureLiveFixtures() {
        runBlocking {
            val deps = dependencies()
            val configuredProbe = deps.probeRepository().getProbeConfig()
                ?: emitNotRun("probe_not_configured")
            if (configuredProbe.ipAddress.isBlank() || configuredProbe.username.isBlank()) {
                emitNotRun("probe_configuration_incomplete")
            }
            if (configuredProbe.testInterface.isBlank()) {
                emitNotRun("probe_interface_not_selected")
            }

            val manager = TestFixtureManager(
                sessionId = "legacy-live-${System.nanoTime()}",
                clients = deps.clientRepository(),
                profiles = deps.testProfileRepository(),
                reports = deps.reportRepository()
            )
            fixtureManager = manager
            val created = manager.createCoreFixtures()
            val liveProfile = created.profile.copy(
                runTdr = configuredProbe.tdrCapability != TdrCapability.UNSUPPORTED,
                runLinkStatus = true,
                runLldp = true,
                runPing = true,
                pingTarget1 = DHCP_GATEWAY_TARGET,
                pingTarget2 = PUBLIC_DNS_TARGET,
                pingCount = 4
            )
            deps.testProfileRepository().updateProfile(liveProfile)
            fixtures = created.copy(profile = liveProfile)
        }
    }

    private fun collectAndRegisterTrace(existingPaths: Set<String>) {
        val deadline = SystemClock.uptimeMillis() + TRACE_FINALIZATION_TIMEOUT_MS
        var trace: File? = null
        while (SystemClock.uptimeMillis() < deadline) {
            trace = traceFiles()
                .filterNot { it.canonicalPath in existingPaths }
                .filter { file ->
                    runCatching {
                        file.useLines { lines ->
                            lines.lastOrNull()?.let(::JSONObject)?.getString("eventType") == "run_finished"
                        }
                    }.getOrDefault(false)
                }
                .maxByOrNull(File::lastModified)
            if (trace != null) break
            SystemClock.sleep(TRACE_FINALIZATION_POLL_MS)
        }

        val completedTrace = requireNotNull(trace) { "Completed debug trace was not produced" }
        val events = completedTrace.readLines()
            .filter(String::isNotBlank)
            .map(::JSONObject)
        check(events.isNotEmpty()) { "Debug trace is empty" }
        check(events.all { it.getString("sessionId") == scenarioRule.sessionId }) {
            "Trace session correlation changed"
        }
        check(events.all { it.getString("scenarioId") == LEGACY_SCENARIO_ID }) {
            "Trace scenario correlation changed"
        }
        val eventTypes = events.map { it.getString("eventType") }.toSet()
        val requiredEvents = setOf(
            "run_started",
            "probe_request",
            "probe_response",
            "parsed_response",
            "normalized_response",
            "normalized_result",
            "test_decision",
            "ui_snapshot",
            "run_finished"
        )
        check(eventTypes.containsAll(requiredEvents)) {
            "Trace is missing required events: ${(requiredEvents - eventTypes).sorted()}"
        }

        val probe = runBlocking { dependencies().probeRepository().getProbeConfig() }
            ?: error("Configured probe disappeared before trace validation")
        val credentialCanaries = setOf(probe.username, probe.password)
            .filter(String::isNotBlank)
            .toSet()
        if (credentialCanaries.isNotEmpty()) {
            check(ArtifactSecretScanner(credentialCanaries).scan(listOf(completedTrace)).isEmpty()) {
                "A configured probe credential reached the debug trace"
            }
        }
        scenarioRule.copyArtifact(
            source = completedTrace,
            filename = "probe-trace.ndjson",
            mediaType = "application/x-ndjson",
            redactionStatus = RedactionStatus.VERIFIED_SCAN
        )
    }

    private suspend fun cleanupLiveFixtures(): com.app.miklink.e2e.support.CleanupResult {
        savedSessionReport?.let { report ->
            runCatching {
                dependencies().reportRepository().getReport(report.reportId)?.let {
                    dependencies().reportRepository().deleteReport(it)
                }
            }.onFailure {
                return com.app.miklink.e2e.support.CleanupResult(
                    CleanupStatus.FAIL,
                    "SAVED_REPORT_CLEANUP_FAILED"
                )
            }
            savedSessionReport = null
        }
        return fixtureManager?.cleanup()
            ?: com.app.miklink.e2e.support.CleanupResult(CleanupStatus.NOT_REQUIRED)
    }

    private fun awaitSavedSessionReport(existingIds: Set<Long>): TestReport {
        val deadline = SystemClock.uptimeMillis() + 10_000L
        while (SystemClock.uptimeMillis() < deadline) {
            val report = runBlocking {
                dependencies().reportRepository().observeAllReports().first()
                    .firstOrNull { it.reportId !in existingIds && it.clientId == fixtures.client.clientId }
            }
            if (report != null) return report
            SystemClock.sleep(100L)
        }
        throw AssertionError("Completed UI result was not persisted to history")
    }

    private fun waitForScrollableTag(tag: String, timeoutMs: Long) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.hasObject(By.res(tag))) return
            device.findObject(By.scrollable(true))?.let { scrollable ->
                runCatching { scrollable.scroll(Direction.DOWN, 0.75f) }
            }
            SystemClock.sleep(100L)
        }
        throw AssertionError("Result section not visible: $tag")
    }

    private fun requireScrollableObject(tag: String): androidx.test.uiautomator.UiObject2 {
        waitForScrollableTag(tag, 10_000L)
        return requireObject(tag)
    }

    private fun traceFiles(): List<File> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), TRACE_DIRECTORY)
        return directory.listFiles { file ->
            file.isFile && file.name.startsWith("debug_trace_") && file.extension == "ndjson"
        }?.toList().orEmpty()
    }

    private fun dependencies(): DebugE2EEntryPoint {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        return EntryPointAccessors.fromApplication(appContext, DebugE2EEntryPoint::class.java)
    }

    private fun selectFixtureFromSheet(id: Long, visibleLabel: String): Long {
        val target = device.wait(Until.findObject(By.text(visibleLabel)), 10_000L)
            ?: emitNotRun("session_fixture_not_visible")
        target.click()
        return id
    }

    private fun waitForTag(tag: String, timeoutMs: Long, reason: String, notRunOnTimeout: Boolean) {
        val found = device.wait(Until.hasObject(By.res(tag)), timeoutMs)
        if (!found) {
            if (notRunOnTimeout) {
                emitNotRun(reason)
            } else {
                throw AssertionError(reason)
            }
        }
    }

    private fun hasVisibleText(text: String): Boolean {
        return device.hasObject(By.text(text))
    }

    private fun requireObject(resourceId: String) =
        device.wait(Until.findObject(By.res(resourceId)), 10_000L)
            ?: throw AssertionError("Required UI object not found: $resourceId")

    private fun emitNotRun(reason: String): Nothing {
        throw NotRunException(reason)
    }

    private fun logE2e(event: String, fields: Map<String, Any?>) {
        val payload = fields.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            val escaped = value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
            "\"$key\":\"$escaped\""
        }
        Log.i(E2E_TAG, "$event $payload")
    }

    private class NotRunException(val reason: String) : RuntimeException(reason)

    private companion object {
        private const val E2E_TAG = "MIKLINK_E2E"
        private const val LEGACY_SCENARIO_ID = "legacy-live-probe"
        private const val TRACE_DIRECTORY = "e2e-trace"
        private const val TRACE_FINALIZATION_TIMEOUT_MS = 10_000L
        private const val TRACE_FINALIZATION_POLL_MS = 100L
        // RunTestUseCase has a 90s timeout; this leaves room for splash and UI transitions on real devices.
        private const val LIVE_RESULT_TIMEOUT_MS = 150_000L
        private const val DHCP_GATEWAY_TARGET = "DHCP_GATEWAY"
        private const val PUBLIC_DNS_TARGET = "8.8.8.8"
    }
}

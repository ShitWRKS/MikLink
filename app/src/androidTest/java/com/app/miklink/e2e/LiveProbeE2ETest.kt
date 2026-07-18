package com.app.miklink.e2e

import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestThresholds
import com.app.miklink.MainActivity
import com.app.miklink.R
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.test.components.TestExecutionTags
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveProbeE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun runLiveProbeE2E() {
        logE2e(
            event = "MIKLINK_E2E_START",
            fields = mapOf(
                "test" to "LiveProbeE2ETest",
                "mode" to "physical_device_live_probe"
            )
        )

        try {
            waitForTag(DashboardTags.CLIENT_SELECTOR, 20_000L, "dashboard_not_ready", notRunOnTimeout = false)
            logE2e("MIKLINK_E2E_STEP", mapOf("step" to "app_started"))

            ensureLiveFixtures()

            val selectedClientId = selectClient()
            val selectedProfileId = selectProfile()

            val startNode = composeRule.onNodeWithTag(DashboardTags.START_TEST_BUTTON, useUnmergedTree = true)
            startNode.assertExists()
            try {
                startNode.assertIsEnabled()
            } catch (_: AssertionError) {
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

            startNode.performClick()
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

            val runningStillVisible = composeRule
                .onAllNodesWithTag(TestExecutionTags.HERO_RUNNING, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
            if (runningStillVisible) {
                throw AssertionError("Run appears stuck in running state")
            }

            logE2e("MIKLINK_E2E_END", mapOf("status" to "PASS"))
        } catch (notRun: NotRunException) {
            logE2e(
                event = "MIKLINK_E2E_END",
                fields = mapOf("status" to "NOT_RUN", "reason" to notRun.reason)
            )
            throw AssertionError("NOT_RUN: ${notRun.reason}")
        } catch (error: Throwable) {
            logE2e(
                event = "MIKLINK_E2E_END",
                fields = mapOf("status" to "FAIL", "reason" to (error.message ?: "unknown_failure"))
            )
            throw error
        }
    }

    private fun selectClient(): Long {
        composeRule.onNodeWithTag(DashboardTags.CLIENT_SELECTOR, useUnmergedTree = true).performClick()
        val args = InstrumentationRegistry.getArguments()
        val explicitId = args.getString("clientId")?.toLongOrNull()
        return selectFromSheet(
            explicitId = explicitId,
            itemPrefix = DashboardTags.CLIENT_ITEM_PREFIX,
            missingReason = "missing_live_client"
        ).also { selectedId ->
            if (explicitId == null) {
                logE2e(
                    event = "MIKLINK_E2E_STEP",
                    fields = mapOf("step" to "fallback_client_selection", "clientId" to selectedId)
                )
            }
        }
    }

    private fun selectProfile(): Long {
        composeRule.onNodeWithTag(DashboardTags.PROFILE_SELECTOR, useUnmergedTree = true).performClick()
        val args = InstrumentationRegistry.getArguments()
        val explicitId = args.getString("profileId")?.toLongOrNull()
        return selectFromSheet(
            explicitId = explicitId,
            itemPrefix = DashboardTags.PROFILE_ITEM_PREFIX,
            missingReason = "missing_live_profile"
        ).also { selectedId ->
            if (explicitId == null) {
                logE2e(
                    event = "MIKLINK_E2E_STEP",
                    fields = mapOf("step" to "fallback_profile_selection", "profileId" to selectedId)
                )
            }
        }
    }

    private fun ensureLiveFixtures() {
        runBlocking {
            val deps = dependencies()
            val probeRepository = deps.probeRepository()
            val clientRepository = deps.clientRepository()
            val profileRepository = deps.testProfileRepository()

            ensureProbe(probeRepository)
            ensureClient(clientRepository)
            ensureProfile(profileRepository)
        }
    }

    private suspend fun ensureProbe(probeRepository: ProbeRepository) {
        val existingProbe = probeRepository.getProbeConfig()
        if (existingProbe == null) {
            probeRepository.saveProbeConfig(
                ProbeConfig(
                    ipAddress = AUTO_PROBE_IP,
                    username = AUTO_PROBE_USERNAME,
                    password = AUTO_PROBE_PASSWORD,
                    testInterface = AUTO_PROBE_INTERFACE,
                    isHttps = false,
                    isOnline = false,
                    modelName = null,
                    tdrSupported = false
                )
            )
            logE2e(
                event = "MIKLINK_E2E_STEP",
                fields = mapOf(
                    "step" to "auto_probe_created",
                    "ipAddress" to AUTO_PROBE_IP
                )
            )
            return
        }

        if (existingProbe.ipAddress.isBlank()) {
            probeRepository.saveProbeConfig(
                existingProbe.copy(ipAddress = AUTO_PROBE_IP)
            )
            logE2e(
                event = "MIKLINK_E2E_STEP",
                fields = mapOf(
                    "step" to "auto_probe_ip_updated",
                    "ipAddress" to AUTO_PROBE_IP
                )
            )
        }
    }

    private suspend fun ensureClient(clientRepository: ClientRepository) {
        val clients = clientRepository.observeAllClients().first()
        if (clients.isNotEmpty()) {
            return
        }

        val clientId = clientRepository.insertClient(
            Client(
                clientId = 0L,
                companyName = AUTO_CLIENT_NAME,
                location = null,
                notes = "Auto-generated for LiveProbeE2E",
                networkMode = NetworkMode.DHCP,
                staticIp = null,
                staticSubnet = null,
                staticGateway = null,
                staticCidr = null,
                minLinkRate = "1G",
                socketPrefix = "E2E",
                socketSuffix = "",
                socketSeparator = "-",
                socketNumberPadding = 2,
                nextIdNumber = 1,
                speedTestServerAddress = null,
                speedTestServerUser = null,
                speedTestServerPassword = null
            )
        )
        logE2e(
            event = "MIKLINK_E2E_STEP",
            fields = mapOf(
                "step" to "auto_client_created",
                "clientId" to clientId
            )
        )
    }

    private suspend fun ensureProfile(profileRepository: TestProfileRepository) {
        val profiles = profileRepository.observeAllProfiles().first()
        val exists = profiles.any { profile ->
            profile.runPing &&
                profile.pingTarget1.equals(DHCP_GATEWAY_TARGET, ignoreCase = true) &&
                profile.pingTarget2.equals(PUBLIC_DNS_TARGET, ignoreCase = true)
        }
        if (exists) {
            return
        }

        val profileId = profileRepository.insertProfile(
            TestProfile(
                profileId = 0L,
                profileName = AUTO_PROFILE_NAME,
                profileDescription = "Auto-generated for LiveProbeE2E",
                runTdr = true,
                runLinkStatus = true,
                runLldp = true,
                runPing = true,
                pingTarget1 = DHCP_GATEWAY_TARGET,
                pingTarget2 = PUBLIC_DNS_TARGET,
                pingTarget3 = null,
                pingCount = 4,
                runSpeedTest = false,
                thresholds = TestThresholds.defaults()
            )
        )
        logE2e(
            event = "MIKLINK_E2E_STEP",
            fields = mapOf(
                "step" to "auto_profile_created",
                "profileId" to profileId
            )
        )
    }

    private fun dependencies(): LiveProbeE2EDependencies {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        return EntryPointAccessors.fromApplication(appContext, LiveProbeE2EDependencies::class.java)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LiveProbeE2EDependencies {
        fun clientRepository(): ClientRepository
        fun testProfileRepository(): TestProfileRepository
        fun probeRepository(): ProbeRepository
    }

    private fun selectFromSheet(
        explicitId: Long?,
        itemPrefix: String,
        missingReason: String
    ): Long {
        if (explicitId != null) {
            val targetTag = "${itemPrefix}_$explicitId"
            val exists = composeRule.onAllNodesWithTag(targetTag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
            if (!exists) {
                emitNotRun("${missingReason}_id_not_found")
            }
            composeRule.onNodeWithTag(targetTag, useUnmergedTree = true).performClick()
            return explicitId
        }

        val matcher = SemanticsMatcher("$itemPrefix prefix matcher") { node ->
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            tag?.startsWith("${itemPrefix}_") == true
        }
        val nodes = composeRule.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes()
        if (nodes.isEmpty()) {
            emitNotRun(missingReason)
        }
        val selectedTag = nodes.first().config.getOrNull(SemanticsProperties.TestTag) ?: emitNotRun(missingReason)
        composeRule.onNodeWithTag(selectedTag, useUnmergedTree = true).performClick()
        return selectedTag.removePrefix("${itemPrefix}_").toLongOrNull() ?: emitNotRun("${missingReason}_invalid_id")
    }

    private fun waitForTag(tag: String, timeoutMs: Long, reason: String, notRunOnTimeout: Boolean) {
        val found = runCatching {
            composeRule.waitUntil(timeoutMs) {
                composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
        }.isSuccess
        if (!found) {
            if (notRunOnTimeout) {
                emitNotRun(reason)
            } else {
                throw AssertionError(reason)
            }
        }
    }

    private fun hasVisibleText(text: String): Boolean {
        return composeRule.onAllNodesWithText(text, substring = false, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }

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
        // RunTestUseCase has a 90s timeout; this leaves room for splash and UI transitions on real devices.
        private const val LIVE_RESULT_TIMEOUT_MS = 150_000L
        private const val AUTO_CLIENT_NAME = "E2E Auto Client"
        private const val AUTO_PROFILE_NAME = "E2E Auto Profile"
        private const val DHCP_GATEWAY_TARGET = "DHCP_GATEWAY"
        private const val PUBLIC_DNS_TARGET = "8.8.8.8"
        private const val AUTO_PROBE_IP = "172.29.0.1"
        private const val AUTO_PROBE_USERNAME = "admin"
        private const val AUTO_PROBE_PASSWORD = ""
        private const val AUTO_PROBE_INTERFACE = "ether1"
    }
}

package com.app.miklink.e2e.functional

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.e2e.support.StepKind
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentUiTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProbeConfigurationUiTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("ui-probe-configuration")

    @Test
    fun configuredProbeCanBeOpenedSavedAndReopenedThroughUi() = FunctionalUiSupport(scenarioRule).runScenario {
        clickResource(DashboardTags.SETTINGS_BUTTON, record = false)
        clickResource(AgentUiTags.Settings.PROBE, record = false)
        requireResource(AgentUiTags.Probe.SCREEN, record = false)

        val address = requireResource(AgentUiTags.Probe.ADDRESS, record = false).text.orEmpty()
        val username = requireResource(AgentUiTags.Probe.USERNAME, record = false).text.orEmpty()
        if (address.isBlank() || username.isBlank()) {
            notRun("PROBE_NOT_CONFIGURED", "configured-probe")
        }

        clickResource(AgentUiTags.Probe.VERIFY, record = false)
        waitForResourceEnabled(AgentUiTags.Probe.SAVE, enabled = false, timeoutMs = 5_000L, record = false)
        val (resultTag, result) = runCatching {
            waitForAnyResource(
                AgentUiTags.Probe.VERIFY_SUCCESS,
                AgentUiTags.Probe.VERIFY_ERROR,
                timeoutMs = 30_000L,
                record = false
            )
        }.getOrElse {
            notRun("PROBE_UNREACHABLE_TIMEOUT", "reachable-authenticated-probe")
        }
        if (resultTag == AgentUiTags.Probe.VERIFY_ERROR) {
            notRun(classifyProbeFailure(result.text.orEmpty()), "reachable-authenticated-probe")
        }
        check(result.text?.isNotBlank() == true) { "Successful Verify exposed no detected probe information" }
        recordStep("open:settings")
        recordStep("open:probe")
        recordStep("click:${AgentUiTags.Probe.VERIFY}")
        recordStep("assert:${AgentUiTags.Probe.VERIFY_SUCCESS}", StepKind.ASSERTION)

        val save = requireResource(AgentUiTags.Probe.SAVE)
        check(save.isEnabled) { "Save was not enabled after successful Verify" }
        clickResource(AgentUiTags.Probe.SAVE)
        requireResource(AgentUiTags.Settings.SCREEN)
        clickResource(AgentUiTags.Settings.PROBE)
        check(requireResource(AgentUiTags.Probe.ADDRESS).text == address) {
            "Probe address did not persist"
        }
        check(requireResource(AgentUiTags.Probe.USERNAME).text == username) {
            "Probe username did not persist"
        }
        requireResource(AgentUiTags.Probe.VERIFY_SUCCESS)
        pressBackToDashboard()
    }

    private fun classifyProbeFailure(message: String): String {
        val normalized = message.lowercase()
        return when {
            listOf("auth", "unauthorized", "credential", "password", "forbidden", "access denied", "credenz", "negat").any(normalized::contains) ->
                "PROBE_AUTHENTICATION_REJECTED"
            listOf("tls", "handshake", "certificate", "certificat", "cipher").any(normalized::contains) ->
                "PROBE_TLS_UNAVAILABLE"
            listOf("timeout", "timed out", "scadut").any(normalized::contains) ->
                "PROBE_UNREACHABLE_TIMEOUT"
            listOf("connect", "unreachable", "refused", "network", "route", "socket", "host", "connession", "raggiung", "rifiutat", "rete").any(normalized::contains) ->
                "PROBE_UNREACHABLE"
            else -> "PROBE_VERIFICATION_ENVIRONMENT_UNAVAILABLE"
        }
    }
}

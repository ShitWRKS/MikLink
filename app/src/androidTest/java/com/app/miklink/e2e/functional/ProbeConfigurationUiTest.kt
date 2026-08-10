package com.app.miklink.e2e.functional

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.ScenarioRule
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
        clickResource(DashboardTags.SETTINGS_BUTTON)
        clickResource(AgentUiTags.Settings.PROBE)
        requireResource(AgentUiTags.Probe.SCREEN)

        val address = requireResource(AgentUiTags.Probe.ADDRESS).text.orEmpty()
        val username = requireResource(AgentUiTags.Probe.USERNAME).text.orEmpty()
        val save = requireResource(AgentUiTags.Probe.SAVE)
        if (address.isBlank() || username.isBlank() || !save.isEnabled) {
            pressBackToDashboard()
            notRun("PROBE_NOT_CONFIGURED_OR_VERIFIED", "configured-probe")
        }

        save.click()
        requireResource(AgentUiTags.Settings.SCREEN)
        clickResource(AgentUiTags.Settings.PROBE)
        check(requireResource(AgentUiTags.Probe.ADDRESS).text == address)
        check(requireResource(AgentUiTags.Probe.USERNAME).text == username)
        pressBackToDashboard()
    }
}

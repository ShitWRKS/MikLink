package com.app.miklink.e2e.functional

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentUiTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileCrudUiTest {
    private val token = "E2EProfile${System.nanoTime()}"
    private val profileName = "E2E Profile $token"
    private val cleanup = SessionRecordCleanup(token)

    @get:Rule val scenarioRule = ScenarioRule.catalog("ui-profile-crud", cleanup = cleanup::run)

    @Test
    fun createToggleConfigureReopenAndEditThroughUi() = FunctionalUiSupport(scenarioRule).runScenario {
        clickResource(DashboardTags.MANAGE_PROFILES)
        clickResource(AgentUiTags.Profile.ADD)
        requireResource(AgentUiTags.Profile.EDIT)
        replaceText(AgentUiTags.Profile.NAME, profileName)
        replaceText(AgentUiTags.Profile.DESCRIPTION, "Primary profile")

        val pingSwitch = clickResource(AgentUiTags.Profile.RUN_PING, scroll = true)
        check(pingSwitch.isChecked) { "Ping toggle did not enable" }
        clickResource(AgentUiTags.Profile.PING_CONFIG, scroll = true)
        replaceText(AgentUiTags.Profile.PING_TARGET_1, "1.1.1.1", scroll = true)
        replaceText(AgentUiTags.Profile.PING_COUNT, "4", scroll = true)
        clickResource(AgentUiTags.Profile.SAVE)

        requireResource(AgentUiTags.Profile.LIST)
        clickText(profileName)
        check(requireResource(AgentUiTags.Profile.DESCRIPTION).text == "Primary profile")
        check(requireResource(AgentUiTags.Profile.RUN_PING, scroll = true).isChecked)
        clickResource(AgentUiTags.Profile.PING_CONFIG, scroll = true)
        check(requireResource(AgentUiTags.Profile.PING_TARGET_1, scroll = true).text == "1.1.1.1")
        check(requireResource(AgentUiTags.Profile.PING_COUNT, scroll = true).text == "4")

        replaceText(AgentUiTags.Profile.DESCRIPTION, "Updated profile", scroll = true)
        clickResource(AgentUiTags.Profile.SAVE)
        requireResource(AgentUiTags.Profile.LIST)
        clickText(profileName)
        check(requireResource(AgentUiTags.Profile.DESCRIPTION, scroll = true).text == "Updated profile") {
            "Edited profile description did not persist through UI"
        }
    }
}

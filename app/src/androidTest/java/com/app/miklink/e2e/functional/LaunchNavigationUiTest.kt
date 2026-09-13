package com.app.miklink.e2e.functional

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentUiTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchNavigationUiTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("ui-launch-navigation")

    @Test
    fun primarySectionsOpenAndReturnToDashboard() = FunctionalUiSupport(scenarioRule).runScenario {
        clickResource(DashboardTags.MANAGE_CLIENTS)
        requireResource(AgentUiTags.Client.LIST)
        pressBackToDashboard()

        clickResource(DashboardTags.MANAGE_PROFILES)
        requireResource(AgentUiTags.Profile.LIST)
        pressBackToDashboard()

        clickResource(DashboardTags.HISTORY_BUTTON)
        requireResource(AgentUiTags.History.SCREEN)
        pressBackToDashboard()

        clickResource(DashboardTags.SETTINGS_BUTTON)
        requireResource(AgentUiTags.Settings.SCREEN)
        pressBackToDashboard()
    }
}

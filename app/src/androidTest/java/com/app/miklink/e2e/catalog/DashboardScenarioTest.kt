package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.ui.dashboard.DashboardTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("dashboard")

    @Test
    fun launchSetupAndSelectionSurfaceIsDiscoverable() {
        assertCatalogMembership("dashboard", FeatureGroup.LAUNCH_DASHBOARD)
        assertEquals(
            setOf(
                DashboardTags.SCREEN,
                DashboardTags.CLIENT_SELECTOR,
                DashboardTags.PROFILE_SELECTOR,
                DashboardTags.START_TEST_BUTTON
            ),
            DashboardTags.stableTags
        )
    }
}

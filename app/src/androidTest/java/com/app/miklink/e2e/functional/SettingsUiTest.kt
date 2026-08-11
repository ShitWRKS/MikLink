package com.app.miklink.e2e.functional

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.catalog.appOnlyDependencies
import com.app.miklink.e2e.support.CleanupResult
import com.app.miklink.e2e.support.CleanupStatus
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentUiTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    private var originalStrategyTag: String? = null
    private var restoredThroughUi = false

    @get:Rule val scenarioRule = ScenarioRule.catalog(
        scenarioId = "ui-settings",
        cleanup = {
            val original = originalStrategyTag
            if (original != null && !restoredThroughUi) {
                val strategy = if (original == AgentUiTags.Settings.ID_STRATEGY_CONTINUOUS) {
                    com.app.miklink.core.domain.model.preferences.IdNumberingStrategy.CONTINUOUS_INCREMENT
                } else {
                    com.app.miklink.core.domain.model.preferences.IdNumberingStrategy.FILL_GAPS
                }
                appOnlyDependencies().userPreferencesRepository().setIdNumberingStrategy(strategy)
            }
            CleanupResult(CleanupStatus.PASS)
        }
    )

    @Test
    fun numberingSelectionPersistsAndIsRestoredThroughUi() = FunctionalUiSupport(scenarioRule).runScenario {
        clickResource(DashboardTags.SETTINGS_BUTTON)
        requireResource(AgentUiTags.Settings.SCREEN)

        clickResource(AgentUiTags.Settings.ID_STRATEGY, scroll = true)
        val continuous = requireResource(AgentUiTags.Settings.ID_STRATEGY_CONTINUOUS)
        val fillGaps = requireResource(AgentUiTags.Settings.ID_STRATEGY_FILL_GAPS)
        check(continuous.isChecked.xor(fillGaps.isChecked)) { "Exactly one numbering strategy must be selected" }
        originalStrategyTag = if (continuous.isChecked) {
            AgentUiTags.Settings.ID_STRATEGY_CONTINUOUS
        } else {
            AgentUiTags.Settings.ID_STRATEGY_FILL_GAPS
        }
        val replacementTag = if (originalStrategyTag == AgentUiTags.Settings.ID_STRATEGY_CONTINUOUS) {
            AgentUiTags.Settings.ID_STRATEGY_FILL_GAPS
        } else {
            AgentUiTags.Settings.ID_STRATEGY_CONTINUOUS
        }

        clickResource(replacementTag)
        pressBackToDashboard()
        clickResource(DashboardTags.SETTINGS_BUTTON)
        requireResource(AgentUiTags.Settings.SCREEN)
        clickResource(AgentUiTags.Settings.ID_STRATEGY, scroll = true)
        check(requireResource(replacementTag).isChecked) { "Replacement numbering strategy did not persist" }

        clickResource(requireNotNull(originalStrategyTag))
        pressBackToDashboard()
        clickResource(DashboardTags.SETTINGS_BUTTON)
        requireResource(AgentUiTags.Settings.SCREEN)
        clickResource(AgentUiTags.Settings.ID_STRATEGY, scroll = true)
        check(requireResource(requireNotNull(originalStrategyTag)).isChecked) {
            "Original numbering strategy was not restored through UI"
        }
        restoredThroughUi = true
    }
}

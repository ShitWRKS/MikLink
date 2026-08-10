package com.app.miklink.e2e.functional

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
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
    private var originalStrategy: IdNumberingStrategy? = null

    @get:Rule val scenarioRule = ScenarioRule.catalog(
        scenarioId = "ui-settings",
        cleanup = {
            originalStrategy?.let { appOnlyDependencies().userPreferencesRepository().setIdNumberingStrategy(it) }
            CleanupResult(CleanupStatus.PASS)
        }
    )

    @Test
    fun numberingSelectionPersistsAndIsRestoredThroughUi() = FunctionalUiSupport(scenarioRule).runScenario {
        clickResource(DashboardTags.SETTINGS_BUTTON)
        requireResource(AgentUiTags.Settings.SCREEN)

        originalStrategy = when {
            device.hasObject(androidx.test.uiautomator.By.text("Continuo")) ->
                IdNumberingStrategy.CONTINUOUS_INCREMENT
            device.hasObject(androidx.test.uiautomator.By.text("Riempi Buchi")) ->
                IdNumberingStrategy.FILL_GAPS
            else -> throw AssertionError("Initial numbering strategy is not visible")
        }
        val replacement = if (originalStrategy == IdNumberingStrategy.CONTINUOUS_INCREMENT) {
            "Riempi Buchi"
        } else {
            "Incremento Continuo"
        }
        val replacementSummary = if (replacement == "Riempi Buchi") "Riempi Buchi" else "Continuo"
        val originalChoice = if (originalStrategy == IdNumberingStrategy.CONTINUOUS_INCREMENT) {
            "Incremento Continuo"
        } else {
            "Riempi Buchi"
        }
        val originalSummary = if (originalStrategy == IdNumberingStrategy.CONTINUOUS_INCREMENT) "Continuo" else "Riempi Buchi"

        clickResource(AgentUiTags.Settings.ID_STRATEGY, scroll = true)
        clickText(replacement)
        pressBackToDashboard()
        clickResource(DashboardTags.SETTINGS_BUTTON)
        requireText(replacementSummary)

        clickResource(AgentUiTags.Settings.ID_STRATEGY, scroll = true)
        clickText(originalChoice)
        pressBackToDashboard()
        clickResource(DashboardTags.SETTINGS_BUTTON)
        requireText(originalSummary)
    }
}

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
class ReportSettingsUiTest {
    private var originalTitle: String? = null
    private var originalIncludeEmpty: Boolean? = null

    @get:Rule val scenarioRule = ScenarioRule.catalog(
        scenarioId = "ui-report-settings",
        cleanup = {
            val preferences = appOnlyDependencies().userPreferencesRepository()
            originalTitle?.let { preferences.setPdfReportTitle(it) }
            originalIncludeEmpty?.let { preferences.setPdfIncludeEmptyTests(it) }
            CleanupResult(CleanupStatus.PASS)
        }
    )

    @Test
    fun titleAndContentPreferencePersistAndRestoreThroughUi() = FunctionalUiSupport(scenarioRule).runScenario {
        clickResource(DashboardTags.SETTINGS_BUTTON)
        clickResource(AgentUiTags.Settings.PDF, scroll = true)
        requireResource(AgentUiTags.Settings.PDF_SCREEN)

        val titleField = requireResource(AgentUiTags.Settings.PDF_TITLE)
        val includeEmptySwitch = requireResource(AgentUiTags.Settings.PDF_INCLUDE_EMPTY, scroll = true)
        originalTitle = titleField.text.orEmpty()
        originalIncludeEmpty = includeEmptySwitch.isChecked
        val changedTitle = "E2E Report ${System.nanoTime()}"
        replaceText(AgentUiTags.Settings.PDF_TITLE, changedTitle)
        clickResource(AgentUiTags.Settings.PDF_INCLUDE_EMPTY, scroll = true)

        device.pressBack()
        requireResource(AgentUiTags.Settings.SCREEN)
        clickResource(AgentUiTags.Settings.PDF, scroll = true)
        check(requireResource(AgentUiTags.Settings.PDF_TITLE).text == changedTitle)
        check(requireResource(AgentUiTags.Settings.PDF_INCLUDE_EMPTY, scroll = true).isChecked != originalIncludeEmpty)

        replaceText(AgentUiTags.Settings.PDF_TITLE, originalTitle.orEmpty(), scroll = true)
        clickResource(AgentUiTags.Settings.PDF_INCLUDE_EMPTY, scroll = true)
        device.pressBack()
        requireResource(AgentUiTags.Settings.SCREEN)
        clickResource(AgentUiTags.Settings.PDF, scroll = true)
        check(requireResource(AgentUiTags.Settings.PDF_TITLE, scroll = true).text == originalTitle.orEmpty())
        check(requireResource(AgentUiTags.Settings.PDF_INCLUDE_EMPTY, scroll = true).isChecked == originalIncludeEmpty)
    }
}

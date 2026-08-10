package com.app.miklink.e2e.functional

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.catalog.appOnlyDependencies
import com.app.miklink.e2e.support.CleanupResult
import com.app.miklink.e2e.support.CleanupStatus
import com.app.miklink.e2e.support.CoreFixtures
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.e2e.support.TestFixtureManager
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentUiTags
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfExportUiTest {
    private var fixtureManager: TestFixtureManager? = null

    @get:Rule val scenarioRule = ScenarioRule.catalog(
        scenarioId = "ui-pdf-export",
        cleanup = { fixtureManager?.cleanup() ?: CleanupResult(CleanupStatus.NOT_REQUIRED) }
    )

    @Test
    fun historyDetailDialogProducesRetrievableValidPdf() {
        val fixtures = createFixtures("ui-pdf-export")
        FunctionalUiSupport(scenarioRule).runScenario {
            clickResource(DashboardTags.HISTORY_BUTTON)
            replaceText(AgentUiTags.History.SEARCH, fixtures.client.companyName)
            clickResource("${AgentUiTags.History.CLIENT_EXPAND_PREFIX}_${fixtures.client.clientId}")
            clickResource("${AgentUiTags.History.REPORT_ITEM_PREFIX}_${fixtures.report.reportId}")
            requireResource(AgentUiTags.Report.SCREEN)

            val before = cachePdfPaths()
            clickResource(AgentUiTags.Report.EXPORT_PDF)
            requireResource(AgentUiTags.Report.PDF_DIALOG)
            clickResource(AgentUiTags.Report.PDF_OPTIONS)
            replaceText(AgentUiTags.Report.PDF_TITLE, "E2E Functional PDF", scroll = true)
            clickResource(AgentUiTags.Report.PDF_ORIENTATION_LANDSCAPE, scroll = true)
            clickResource(AgentUiTags.Report.PDF_CONFIRM)

            val pdf = newestPdfNotIn(before)
            registerPdf(pdf)
            if (!device.hasObject(androidx.test.uiautomator.By.res(AgentUiTags.Report.SCREEN))) {
                device.pressBack()
            }
            requireResource(AgentUiTags.Report.SCREEN)
        }
    }

    private fun createFixtures(name: String): CoreFixtures = runBlocking {
        val dependencies = appOnlyDependencies()
        TestFixtureManager(
            sessionId = "$name-${System.nanoTime()}",
            clients = dependencies.clientRepository(),
            profiles = dependencies.testProfileRepository(),
            reports = dependencies.reportRepository()
        ).also { fixtureManager = it }.createCoreFixtures()
    }
}

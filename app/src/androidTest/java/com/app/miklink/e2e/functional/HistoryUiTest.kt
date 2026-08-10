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
class HistoryUiTest {
    private var fixtureManager: TestFixtureManager? = null

    @get:Rule val scenarioRule = ScenarioRule.catalog(
        scenarioId = "ui-history",
        cleanup = { fixtureManager?.cleanup() ?: CleanupResult(CleanupStatus.NOT_REQUIRED) }
    )

    @Test
    fun searchOpenDetailAndDeleteRunThroughUi() {
        val fixtures = createFixtures("ui-history")
        FunctionalUiSupport(scenarioRule).runScenario {
            clickResource(DashboardTags.HISTORY_BUTTON)
            requireResource(AgentUiTags.History.SCREEN)
            replaceText(AgentUiTags.History.SEARCH, fixtures.client.companyName)
            clickResource("${AgentUiTags.History.CLIENT_EXPAND_PREFIX}_${fixtures.client.clientId}")
            clickResource("${AgentUiTags.History.REPORT_ITEM_PREFIX}_${fixtures.report.reportId}")
            requireResource(AgentUiTags.Report.SCREEN)
            requireText(requireNotNull(fixtures.report.socketName))

            clickResource(AgentUiTags.Report.DELETE, scroll = true)
            clickResource(AgentUiTags.Report.DELETE_CONFIRM)
            requireResource(AgentUiTags.History.SCREEN)
            assertResourceAbsent("${AgentUiTags.History.REPORT_ITEM_PREFIX}_${fixtures.report.reportId}")
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

package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.ScenarioRule
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryReportScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("history-report")

    @Test
    fun searchDetailDeleteDuplicateAndRepeatDataRemainConsistent() = withCoreFixtures("history-report", scenarioRule::recordCleanup) { deps, fixtures ->
        assertCatalogMembership("history-report", FeatureGroup.HISTORY_REPORTS)
        val reports = deps.reportRepository()
        val visible = reports.observeReportsByClient(fixtures.client.clientId).first()
        assertTrue(visible.any { it.reportId == fixtures.report.reportId })
        assertNotNull(reports.getReport(fixtures.report.reportId))

        val duplicateId = reports.saveReport(
            fixtures.report.copy(
                reportId = 0,
                timestamp = fixtures.report.timestamp + 1,
                socketName = "${fixtures.report.socketName}-duplicate"
            )
        )
        val duplicate = requireNotNull(reports.getReport(duplicateId))
        assertEquals(fixtures.report.clientId, duplicate.clientId)
        assertEquals(fixtures.report.profileName, duplicate.profileName)

        reports.deleteReport(duplicate)
        assertNull(reports.getReport(duplicateId))
    }
}

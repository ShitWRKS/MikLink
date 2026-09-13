package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.ScenarioRule
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("backup-round-trip")

    @Test
    fun exportImportRoundTripUsesOnlyAppDataAndSessionOwnedFixture() {
        assertCatalogMembership("backup-round-trip", FeatureGroup.BACKUP)
        if (!disposableStateAllowed()) {
            scenarioRule.notRun("DISPOSABLE_LOCAL_STATE_NOT_AUTHORIZED", "disposable-local-state")
        }

        withCoreFixtures("backup-round-trip", scenarioRule::recordCleanup) { deps, fixtures ->

            val json = deps.backupRepository().exportConfigToJson()
            assertTrue(json.contains(fixtures.client.companyName))
            assertTrue(json.contains(fixtures.profile.profileName))
            assertFalse(json.contains("agent-tests"))
            assertFalse(json.contains("screenshot.png"))

            deps.backupRepository().importConfigFromJson(json).getOrThrow()
            assertTrue(deps.clientRepository().observeAllClients().first().any { it.companyName == fixtures.client.companyName })
            assertTrue(deps.testProfileRepository().observeAllProfiles().first().any { it.profileName == fixtures.profile.profileName })

            deps.reportRepository().observeAllReports().first()
                .filter { it.socketName == fixtures.report.socketName }
                .forEach { deps.reportRepository().deleteReport(it) }
            deps.testProfileRepository().observeAllProfiles().first()
                .filter { it.profileName == fixtures.profile.profileName }
                .forEach { deps.testProfileRepository().deleteProfile(it) }
            deps.clientRepository().observeAllClients().first()
                .filter { it.companyName == fixtures.client.companyName }
                .forEach { deps.clientRepository().deleteClient(it) }
        }
    }
}

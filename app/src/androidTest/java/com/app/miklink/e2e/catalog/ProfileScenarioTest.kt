package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.domain.model.GatewayUnresolvedPolicy
import com.app.miklink.core.domain.usecase.testprofile.SaveTestProfileUseCaseImpl
import com.app.miklink.e2e.support.ScenarioRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("profile-crud")

    @Test
    fun flagsTargetsThresholdsCrudRoundTrip() = withCoreFixtures("profile-crud", scenarioRule::recordCleanup) { deps, fixtures ->
        assertCatalogMembership("profile-crud", FeatureGroup.TEST_PROFILES)
        val repository = deps.testProfileRepository()
        val updated = fixtures.profile.copy(
            runTdr = true,
            runPing = true,
            pingTarget1 = "1.1.1.1",
            pingCount = 4,
            thresholds = fixtures.profile.thresholds.copy(gatewayPolicy = GatewayUnresolvedPolicy.SKIP)
        )
        repository.updateProfile(updated)
        assertEquals(updated, repository.getProfile(updated.profileId))

        val invalid = updated.copy(profileName = "", runTdr = false, runLinkStatus = false, runPing = false)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { SaveTestProfileUseCaseImpl(repository)(invalid) }
        }

        repository.deleteProfile(updated)
        assertNull(repository.getProfile(updated.profileId))
    }
}

package com.app.miklink.e2e.functional

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.catalog.appOnlyDependencies
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentUiTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

@RunWith(AndroidJUnit4::class)
class ProfileCrudUiTest {
    private val token = "E2EProfile${System.nanoTime()}"
    private val profileName = "E2E Profile $token"
    private val cleanup = SessionRecordCleanup(token)

    @get:Rule val scenarioRule = ScenarioRule.catalog("ui-profile-crud", cleanup = cleanup::run)

    @Test
    fun createToggleConfigureReopenEditAndDeleteThroughUi() = FunctionalUiSupport(scenarioRule).runScenario {
        clickResource(DashboardTags.MANAGE_PROFILES)
        clickResource(AgentUiTags.Profile.ADD)
        requireResource(AgentUiTags.Profile.EDIT)
        replaceText(AgentUiTags.Profile.NAME, profileName)
        replaceText(AgentUiTags.Profile.DESCRIPTION, "Primary profile")

        val pingSwitch = clickResource(AgentUiTags.Profile.RUN_PING, scroll = true)
        check(pingSwitch.isChecked) { "Ping toggle did not enable" }
        clickResource(AgentUiTags.Profile.PING_CONFIG, scroll = true)
        replaceText(AgentUiTags.Profile.PING_TARGET_1, "1.1.1.1", scroll = true)
        replaceText(AgentUiTags.Profile.PING_COUNT, "4", scroll = true)
        clickResource(AgentUiTags.Profile.SAVE)

        requireResource(AgentUiTags.Profile.LIST)
        val profileId = runBlocking {
            withTimeout(10_000L) {
                appOnlyDependencies().testProfileRepository().observeAllProfiles()
                    .first { profiles -> profiles.any { it.profileName == profileName } }
                    .single { it.profileName == profileName }
                    .profileId
            }
        }
        clickResource("${AgentUiTags.Profile.ITEM_PREFIX}_$profileId", scroll = true)
        requireResource(AgentUiTags.Profile.EDIT)
        scrollToTop()
        check(requireResource(AgentUiTags.Profile.DESCRIPTION).text == "Primary profile")
        check(requireResource(AgentUiTags.Profile.RUN_PING, scroll = true).isChecked)
        clickResource(AgentUiTags.Profile.PING_CONFIG, scroll = true)
        check(requireResource(AgentUiTags.Profile.PING_TARGET_1, scroll = true).text == "1.1.1.1")
        check(requireResource(AgentUiTags.Profile.PING_COUNT, scroll = true).text == "4")

        scrollToTop()
        replaceText(AgentUiTags.Profile.DESCRIPTION, "Updated profile")
        clickResource(AgentUiTags.Profile.SAVE)
        requireResource(AgentUiTags.Profile.LIST)
        clickResource("${AgentUiTags.Profile.ITEM_PREFIX}_$profileId", scroll = true)
        requireResource(AgentUiTags.Profile.EDIT)
        scrollToTop()
        check(requireResource(AgentUiTags.Profile.DESCRIPTION, scroll = true).text == "Updated profile") {
            "Edited profile description did not persist through UI"
        }

        pressBack()
        requireResource(AgentUiTags.Profile.LIST)
        clickResource("${AgentUiTags.Profile.DELETE_PREFIX}_$profileId", scroll = true)
        clickResource(AgentUiTags.Profile.DELETE_CONFIRM)
        assertResourceAbsent("${AgentUiTags.Profile.ITEM_PREFIX}_$profileId", timeoutMs = 10_000L)
    }
}

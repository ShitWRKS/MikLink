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

        clickResource(AgentUiTags.Profile.TAB_LINK)
        check(requireResource(AgentUiTags.Profile.RUN_LINK).isChecked) {
            "Link Status default did not remain enabled"
        }
        check(requireResource(AgentUiTags.Profile.LINK_MIN_RATE).text == "1G")
        requireResource(AgentUiTags.Profile.LINK_MIN_RATE_SLIDER, scroll = true)
        val tdrSwitch = clickResource(AgentUiTags.Profile.RUN_TDR, scroll = true)
        check(tdrSwitch.isChecked) { "TDR toggle did not enable" }

        clickResource(AgentUiTags.Profile.TAB_PING)
        val pingSwitch = clickResource(AgentUiTags.Profile.RUN_PING)
        check(pingSwitch.isChecked) { "Ping toggle did not enable" }
        replaceText(AgentUiTags.Profile.PING_TARGET_1, "1.1.1.1", scroll = true)
        replaceText(AgentUiTags.Profile.PING_COUNT, "4", scroll = true)
        replaceText(AgentUiTags.Profile.PING_LOCAL_MAX_AVG_RTT, "35", scroll = true)
        requireResource(AgentUiTags.Profile.PING_LOCAL_MAX_AVG_RTT_SLIDER, scroll = true)

        clickResource(AgentUiTags.Profile.TAB_LINK)
        clickResource(AgentUiTags.Profile.TAB_PING)
        check(requireResource(AgentUiTags.Profile.PING_TARGET_1, scroll = true).text == "1.1.1.1") {
            "Unsaved Ping target was lost after changing tabs"
        }
        check(requireResource(AgentUiTags.Profile.PING_LOCAL_MAX_AVG_RTT, scroll = true).text == "35") {
            "Unsaved Ping threshold was lost after changing tabs"
        }
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
        requireResource(AgentUiTags.Profile.TAB_GENERAL)
        check(requireResource(AgentUiTags.Profile.DESCRIPTION).text == "Primary profile")
        clickResource(AgentUiTags.Profile.TAB_LINK)
        check(requireResource(AgentUiTags.Profile.RUN_TDR, scroll = true).isChecked)
        clickResource(AgentUiTags.Profile.TAB_PING)
        check(requireResource(AgentUiTags.Profile.RUN_PING).isChecked)
        check(requireResource(AgentUiTags.Profile.PING_TARGET_1, scroll = true).text == "1.1.1.1")
        check(requireResource(AgentUiTags.Profile.PING_COUNT, scroll = true).text == "4")
        check(requireResource(AgentUiTags.Profile.PING_LOCAL_MAX_AVG_RTT, scroll = true).text.toDouble() == 35.0)

        replaceText(AgentUiTags.Profile.PING_LOCAL_MAX_AVG_RTT, "40", scroll = true)
        clickResource(AgentUiTags.Profile.TAB_GENERAL)
        replaceText(AgentUiTags.Profile.DESCRIPTION, "Updated profile")
        clickResource(AgentUiTags.Profile.SAVE)
        requireResource(AgentUiTags.Profile.LIST)
        clickResource("${AgentUiTags.Profile.ITEM_PREFIX}_$profileId", scroll = true)
        requireResource(AgentUiTags.Profile.EDIT)
        requireResource(AgentUiTags.Profile.TAB_GENERAL)
        check(requireResource(AgentUiTags.Profile.DESCRIPTION, scroll = true).text == "Updated profile") {
            "Edited profile description did not persist through UI"
        }
        clickResource(AgentUiTags.Profile.TAB_PING)
        check(requireResource(AgentUiTags.Profile.PING_LOCAL_MAX_AVG_RTT, scroll = true).text.toDouble() == 40.0) {
            "Edited Ping threshold did not persist through UI"
        }

        pressBack()
        requireResource(AgentUiTags.Profile.LIST)
        clickResource("${AgentUiTags.Profile.DELETE_PREFIX}_$profileId", scroll = true)
        clickResource(AgentUiTags.Profile.DELETE_CONFIRM)
        assertResourceAbsent("${AgentUiTags.Profile.ITEM_PREFIX}_$profileId", timeoutMs = 10_000L)
    }
}

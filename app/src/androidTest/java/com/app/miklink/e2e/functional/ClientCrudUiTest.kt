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
class ClientCrudUiTest {
    private val token = "E2EClient${System.nanoTime()}"
    private val clientName = "E2E Client $token"
    private val cleanup = SessionRecordCleanup(token)

    @get:Rule val scenarioRule = ScenarioRule.catalog("ui-client-crud", cleanup = cleanup::run)

    @Test
    fun createReopenEditDeleteAndValidateStaticNetworkThroughUi() = FunctionalUiSupport(scenarioRule).runScenario {
        clickResource(DashboardTags.MANAGE_CLIENTS)
        clickResource(AgentUiTags.Client.ADD)
        requireResource(AgentUiTags.Client.EDIT)

        replaceText(AgentUiTags.Client.NAME, clientName)
        replaceText(AgentUiTags.Client.LOCATION, "Rack A")
        clickResource(AgentUiTags.Client.NETWORK_STATIC, scroll = true)
        replaceText(AgentUiTags.Client.STATIC_CIDR, "invalid", scroll = true)
        replaceText(AgentUiTags.Client.STATIC_GATEWAY, "invalid", scroll = true)
        check(!requireResource(AgentUiTags.Client.SAVE).isEnabled) {
            "Invalid static network unexpectedly enabled Save"
        }

        replaceText(AgentUiTags.Client.STATIC_CIDR, "192.0.2.10/24", scroll = true)
        replaceText(AgentUiTags.Client.STATIC_GATEWAY, "192.0.2.1", scroll = true)
        check(requireResource(AgentUiTags.Client.SAVE).isEnabled) {
            "Valid static network did not enable Save"
        }
        clickResource(AgentUiTags.Client.SAVE)
        requireResource(AgentUiTags.Client.LIST)
        val clientId = runBlocking {
            withTimeout(10_000L) {
                appOnlyDependencies().clientRepository().observeAllClients()
                    .first { clients -> clients.any { it.companyName == clientName } }
                    .single { it.companyName == clientName }
                    .clientId
            }
        }
        clickResource("${AgentUiTags.Client.ITEM_PREFIX}_$clientId")

        scrollToTop()
        check(requireResource(AgentUiTags.Client.LOCATION).text == "Rack A")
        check(requireResource(AgentUiTags.Client.STATIC_CIDR, scroll = true).text == "192.0.2.10/24")
        check(requireResource(AgentUiTags.Client.STATIC_GATEWAY, scroll = true).text == "192.0.2.1")
        replaceText(AgentUiTags.Client.LOCATION, "Rack B", scroll = true)
        clickResource(AgentUiTags.Client.SAVE)

        requireResource(AgentUiTags.Client.LIST)
        replaceText(AgentUiTags.Client.SEARCH, clientName)
        clickResource("${AgentUiTags.Client.ITEM_PREFIX}_$clientId")
        scrollToTop()
        check(requireResource(AgentUiTags.Client.LOCATION, scroll = true).text == "Rack B") {
            "Edited client location did not persist through UI"
        }

        pressBack()
        requireResource(AgentUiTags.Client.LIST)
        replaceText(AgentUiTags.Client.SEARCH, clientName)
        clickResource("${AgentUiTags.Client.DELETE_PREFIX}_$clientId")
        clickResource(AgentUiTags.Client.DELETE_CONFIRM)
        assertResourceAbsent("${AgentUiTags.Client.ITEM_PREFIX}_$clientId", timeoutMs = 10_000L)
    }
}

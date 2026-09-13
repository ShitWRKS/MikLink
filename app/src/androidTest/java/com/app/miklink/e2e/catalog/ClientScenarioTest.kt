package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.usecase.client.SaveClientUseCaseImpl
import com.app.miklink.e2e.support.ScenarioRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("client-crud")

    @Test
    fun sessionOwnedClientCrudAndStaticValidationRoundTrip() = withCoreFixtures("client-crud", scenarioRule::recordCleanup) { deps, fixtures ->
        assertCatalogMembership("client-crud", FeatureGroup.CLIENTS)
        val repository = deps.clientRepository()
        val updated = fixtures.client.copy(companyName = "${fixtures.client.companyName}-updated", notes = "round-trip")
        repository.updateClient(updated)
        assertEquals(updated, repository.getClient(updated.clientId))

        val invalidStatic = updated.copy(networkMode = NetworkMode.STATIC, staticCidr = "bad", staticGateway = "also-bad")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { SaveClientUseCaseImpl(repository)(invalidStatic) }
        }

        repository.deleteClient(updated)
        assertNull(repository.getClient(updated.clientId))
    }
}

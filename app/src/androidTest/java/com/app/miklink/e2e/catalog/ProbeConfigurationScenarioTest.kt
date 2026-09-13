package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.ui.testing.AgentUiTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProbeConfigurationScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("probe-configuration")

    @Test
    fun configurationSurfaceDoesNotCreateOrReplaceAProbeWithoutVerification() = runBlocking {
        assertCatalogMembership("probe-configuration", FeatureGroup.PROBE_CONFIGURATION)
        val repository = appOnlyDependencies().probeRepository()
        val before = repository.getProbeConfig()

        assertTrue(AgentUiTags.stableTags.containsAll(setOf(
            AgentUiTags.Probe.ADDRESS,
            AgentUiTags.Probe.USERNAME,
            AgentUiTags.Probe.PASSWORD,
            AgentUiTags.Probe.VERIFY,
            AgentUiTags.Probe.SAVE
        )))
        assertEquals("Probe-independent discovery must not install a fallback probe", before, repository.getProbeConfig())
    }
}

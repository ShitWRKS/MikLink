package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import com.app.miklink.e2e.support.ScenarioRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("settings")

    @Test
    fun pollingGlowNumberingProtocolAndPdfPreferencesRoundTrip() = runBlocking {
        assertCatalogMembership("settings", FeatureGroup.SETTINGS)
        val repository = appOnlyDependencies().userPreferencesRepository()
        val original = PreferenceSnapshot.capture(repository)
        try {
            repository.setIdNumberingStrategy(IdNumberingStrategy.FILL_GAPS)
            repository.setProbePollingInterval(7_000L)
            repository.setDashboardGlowIntensity(0.42f)
            repository.setNeighborDiscoveryProtocols(setOf("lldp", "cdp"))
            repository.setPdfReportTitle("e2e-title")

            assertEquals(IdNumberingStrategy.FILL_GAPS, repository.idNumberingStrategy.first())
            assertEquals(7_000L, repository.probePollingInterval.first())
            assertEquals(0.42f, repository.dashboardGlowIntensity.first())
            assertEquals(setOf("lldp", "cdp"), repository.neighborDiscoveryProtocols.first())
            assertEquals("e2e-title", repository.pdfReportTitle.first())
        } finally {
            original.restore(repository)
        }
    }

    private data class PreferenceSnapshot(
        val strategy: IdNumberingStrategy,
        val includeEmpty: Boolean,
        val columns: Set<String>,
        val title: String,
        val hideEmpty: Boolean,
        val glow: Float,
        val polling: Long,
        val protocols: Set<String>
    ) {
        suspend fun restore(repository: com.app.miklink.core.data.repository.preferences.UserPreferencesRepository) {
            repository.setIdNumberingStrategy(strategy)
            repository.setPdfIncludeEmptyTests(includeEmpty)
            repository.setPdfSelectedColumns(columns)
            repository.setPdfReportTitle(title)
            repository.setPdfHideEmptyColumns(hideEmpty)
            repository.setDashboardGlowIntensity(glow)
            repository.setProbePollingInterval(polling)
            repository.setNeighborDiscoveryProtocols(protocols)
        }

        companion object {
            suspend fun capture(repository: com.app.miklink.core.data.repository.preferences.UserPreferencesRepository) =
                PreferenceSnapshot(
                    repository.idNumberingStrategy.first(),
                    repository.pdfIncludeEmptyTests.first(),
                    repository.pdfSelectedColumns.first(),
                    repository.pdfReportTitle.first(),
                    repository.pdfHideEmptyColumns.first(),
                    repository.dashboardGlowIntensity.first(),
                    repository.probePollingInterval.first(),
                    repository.neighborDiscoveryProtocols.first()
                )
        }
    }
}

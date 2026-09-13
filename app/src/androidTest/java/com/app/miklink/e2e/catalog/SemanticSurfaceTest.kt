package com.app.miklink.e2e.catalog

import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.test.components.TestExecutionTags
import com.app.miklink.ui.testing.AgentUiTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticSurfaceTest {
    @Test
    fun stableTagsAreUniqueAndResourceIdSafe() {
        val groups = listOf(
            DashboardTags.stableTags,
            TestExecutionTags.stableTags,
            AgentUiTags.stableTags
        )
        val flattened = groups.flatten()

        assertEquals("Every stable semantic identifier must be globally unique", flattened.size, flattened.toSet().size)
        assertTrue(flattened.all { it.matches(Regex("[a-z][a-z0-9_]*")) })
    }

    @Test
    fun everyVisibleFeatureFlowHasAReachableSemanticSurface() {
        val reachableRoots = mapOf(
            FeatureGroup.LAUNCH_DASHBOARD to DashboardTags.SCREEN,
            FeatureGroup.PROBE_CONFIGURATION to AgentUiTags.Probe.SCREEN,
            FeatureGroup.CLIENTS to AgentUiTags.Client.LIST,
            FeatureGroup.TEST_PROFILES to AgentUiTags.Profile.LIST,
            FeatureGroup.TEST_EXECUTION to TestExecutionTags.SCREEN,
            FeatureGroup.HISTORY_REPORTS to AgentUiTags.History.SCREEN,
            FeatureGroup.PDF_EXPORT to AgentUiTags.Settings.PDF_SCREEN,
            FeatureGroup.BACKUP to AgentUiTags.Settings.BACKUP_SCREEN,
            FeatureGroup.SETTINGS to AgentUiTags.Settings.SCREEN,
            FeatureGroup.RESULT_PRESENTATION to TestExecutionTags.HERO_COMPLETED
        )

        assertEquals(FeatureGroup.entries.toSet(), reachableRoots.keys)
        assertTrue(reachableRoots.values.all { it.isNotBlank() })
    }
}

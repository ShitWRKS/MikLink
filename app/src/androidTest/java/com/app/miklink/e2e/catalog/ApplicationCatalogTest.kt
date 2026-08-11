package com.app.miklink.e2e.catalog

import com.app.miklink.e2e.support.TerminalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ApplicationCatalogTest {
    @Test
    fun targetedAndFullSelectionAreDeterministic() {
        val targeted = E2ETestCatalog.select(setOf("client-crud", "settings"), appOnly = true)
        assertEquals(listOf("client-crud", "settings"), targeted.map { it.id })
        assertTrue(E2ETestCatalog.select(appOnly = true).all { !it.requiresLiveProbe })
        assertThrows(IllegalArgumentException::class.java) {
            E2ETestCatalog.select(setOf("unknown"), appOnly = true)
        }
    }

    @Test
    fun oneFailureDoesNotPreventLaterScenarioAndControlsAggregateOutcome() {
        val selected = E2ETestCatalog.select(setOf("dashboard", "client-crud", "settings"), appOnly = true)
        val executed = mutableListOf<String>()
        val summary = E2ETestCatalog.runContinuing(selected) { scenario ->
            executed += scenario.id
            if (scenario.id == "client-crud") error("intentional")
            CatalogOutcome(scenario.id, TerminalStatus.PASS, "ASSERTIONS_PASSED")
        }
        assertEquals(selected.map { it.id }, executed)
        assertEquals(TerminalStatus.FAIL, summary.aggregateStatus)
        assertEquals(selected.size, summary.outcomes.size)
    }

    @Test
    fun inventoryAccountsForEveryFeatureGroup() {
        val accounted = E2ETestCatalog.scenarios.flatMap { it.featureGroups }.toSet()
        assertEquals(FeatureGroup.entries.toSet(), accounted)
        assertFalse(E2ETestCatalog.scenarios.any { it.id.isBlank() })
    }

    @Test
    fun functionalUiCoverageIsDistinctFromIntegrationCoverage() {
        val functional = E2ETestCatalog.functionalUi()
        assertTrue(functional.isNotEmpty())
        assertTrue(functional.all { it.coverageLevel == CoverageLevel.FUNCTIONAL_UI })
        assertFalse(E2ETestCatalog.find("client-crud")?.coverageLevel == CoverageLevel.FUNCTIONAL_UI)
        assertTrue(E2ETestCatalog.find("ui-client-crud")?.coverageLevel == CoverageLevel.FUNCTIONAL_UI)
    }
}

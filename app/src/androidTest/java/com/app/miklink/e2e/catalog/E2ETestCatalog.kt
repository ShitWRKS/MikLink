package com.app.miklink.e2e.catalog

/** Stable catalog identity used by filtering, evidence, and coverage accounting. */
enum class FeatureGroup(val id: String) {
    LAUNCH_DASHBOARD("FG-01"),
    PROBE_CONFIGURATION("FG-02"),
    CLIENTS("FG-03"),
    TEST_PROFILES("FG-04"),
    TEST_EXECUTION("FG-05"),
    HISTORY_REPORTS("FG-06"),
    PDF_EXPORT("FG-07"),
    BACKUP("FG-08"),
    SETTINGS("FG-09"),
    RESULT_PRESENTATION("FG-10")
}

data class CatalogScenario(
    val id: String,
    val featureGroups: Set<FeatureGroup>,
    val requiresLiveProbe: Boolean = false,
    val requiresSpeedServer: Boolean = false,
    val disruptive: Boolean = false
)

data class CatalogOutcome(
    val scenarioId: String,
    val status: com.app.miklink.e2e.support.TerminalStatus,
    val reasonCode: String
)

data class CatalogRunSummary(val outcomes: List<CatalogOutcome>) {
    val aggregateStatus: com.app.miklink.e2e.support.TerminalStatus = when {
        outcomes.any { it.status == com.app.miklink.e2e.support.TerminalStatus.FAIL } ->
            com.app.miklink.e2e.support.TerminalStatus.FAIL
        outcomes.any { it.status == com.app.miklink.e2e.support.TerminalStatus.PASS } ->
            com.app.miklink.e2e.support.TerminalStatus.PASS
        outcomes.any { it.status == com.app.miklink.e2e.support.TerminalStatus.NOT_RUN } ->
            com.app.miklink.e2e.support.TerminalStatus.NOT_RUN
        else -> com.app.miklink.e2e.support.TerminalStatus.SKIP
    }
}

object E2ETestCatalog {
    val scenarios: List<CatalogScenario> = listOf(
        CatalogScenario("dashboard", setOf(FeatureGroup.LAUNCH_DASHBOARD)),
        CatalogScenario("probe-configuration", setOf(FeatureGroup.PROBE_CONFIGURATION)),
        CatalogScenario("client-crud", setOf(FeatureGroup.CLIENTS)),
        CatalogScenario("profile-crud", setOf(FeatureGroup.TEST_PROFILES)),
        CatalogScenario("history-report", setOf(FeatureGroup.HISTORY_REPORTS)),
        CatalogScenario("pdf-export", setOf(FeatureGroup.PDF_EXPORT)),
        CatalogScenario("backup-round-trip", setOf(FeatureGroup.BACKUP)),
        CatalogScenario("settings", setOf(FeatureGroup.SETTINGS)),
        CatalogScenario("result-presentation", setOf(FeatureGroup.RESULT_PRESENTATION)),
        CatalogScenario(
            "live-probe",
            setOf(FeatureGroup.TEST_EXECUTION, FeatureGroup.RESULT_PRESENTATION),
            requiresLiveProbe = true
        ),
        CatalogScenario(
            "live-speed",
            setOf(FeatureGroup.TEST_EXECUTION),
            requiresLiveProbe = true,
            requiresSpeedServer = true
        ),
        CatalogScenario(
            "connectivity-recovery",
            setOf(FeatureGroup.TEST_EXECUTION),
            requiresLiveProbe = true,
            disruptive = true
        )
    )

    init {
        require(scenarios.map { it.id }.distinct().size == scenarios.size) {
            "Scenario IDs must be unique"
        }
        require(FeatureGroup.entries.all { group -> scenarios.any { group in it.featureGroups } }) {
            "Every feature group must be represented by a scenario"
        }
    }

    fun appOnly(): List<CatalogScenario> = scenarios.filterNot { it.requiresLiveProbe }

    fun find(id: String): CatalogScenario? = scenarios.firstOrNull { it.id == id }

    fun select(ids: Set<String>? = null, appOnly: Boolean = false): List<CatalogScenario> {
        val eligible = if (appOnly) appOnly() else scenarios
        if (ids.isNullOrEmpty()) return eligible
        val unknown = ids - scenarios.map { it.id }.toSet()
        require(unknown.isEmpty()) { "Unknown scenario ids: ${unknown.sorted().joinToString()}" }
        return eligible.filter { it.id in ids }
    }

    inline fun runContinuing(
        selected: List<CatalogScenario>,
        execute: (CatalogScenario) -> CatalogOutcome
    ): CatalogRunSummary = CatalogRunSummary(
        selected.map { scenario ->
            runCatching { execute(scenario) }.getOrElse { failure ->
                CatalogOutcome(
                    scenarioId = scenario.id,
                    status = com.app.miklink.e2e.support.TerminalStatus.FAIL,
                    reasonCode = failure.message ?: "UNCAUGHT_SCENARIO_FAILURE"
                )
            }
        }
    )
}

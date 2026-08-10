package com.app.miklink.e2e.support

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

data class UiObservation(val claim: String, val evidencePaths: Set<String>)

data class UiActionEvidence(
    val actionId: String,
    val beforePaths: Set<String>,
    val afterPaths: Set<String>
)

data class UiReviewEvidence(
    val status: TerminalStatus,
    val reasonCode: String,
    val artifacts: Set<String>,
    val observations: List<UiObservation>,
    val actions: List<UiActionEvidence>,
    val unseenStates: Map<String, String>
) {
    fun violations(): List<String> = buildList {
        if (reasonCode.isBlank()) add("terminal reason is missing")
        observations.forEach { observation ->
            if (observation.claim.isBlank()) add("observation claim is blank")
            if (observation.evidencePaths.isEmpty()) add("${observation.claim}: evidence is missing")
            val missing = observation.evidencePaths - artifacts
            if (missing.isNotEmpty()) add("${observation.claim}: unregistered evidence $missing")
        }
        actions.forEach { action ->
            if (action.actionId.isBlank()) add("action id is blank")
            if (action.beforePaths.isEmpty()) add("${action.actionId}: before evidence is missing")
            if (action.afterPaths.isEmpty()) add("${action.actionId}: after evidence is missing")
            if ((action.beforePaths intersect action.afterPaths).isNotEmpty()) {
                add("${action.actionId}: before and after evidence must be distinct")
            }
            val missing = (action.beforePaths + action.afterPaths) - artifacts
            if (missing.isNotEmpty()) add("${action.actionId}: unregistered evidence $missing")
        }
        if (status == TerminalStatus.NOT_RUN && unseenStates.isEmpty()) {
            add("NOT_RUN review must name unseen state and reason")
        }
        unseenStates.forEach { (state, reason) ->
            if (state.isBlank() || reason.isBlank()) add("unseen state and reason must be explicit")
            if (observations.any { it.claim.contains(state, ignoreCase = true) }) {
                add("unseen state $state is described as observed")
            }
        }
    }
}

@RunWith(AndroidJUnit4::class)
class UiReviewEvidenceTest {
    @Test
    fun reachableReviewLinksFactsAndCorrelatesDistinctBeforeAfterArtifacts() {
        val evidence = UiReviewEvidence(
            status = TerminalStatus.PASS,
            reasonCode = "OBSERVATIONS_EVIDENCED",
            artifacts = setOf("before.xml", "before.png", "after.xml", "after.png"),
            observations = listOf(
                UiObservation("Dashboard selectors are present", setOf("before.xml", "before.png")),
                UiObservation("Settings screen is present after the action", setOf("after.xml", "after.png"))
            ),
            actions = listOf(
                UiActionEvidence("open-settings", setOf("before.xml", "before.png"), setOf("after.xml", "after.png"))
            ),
            unseenStates = emptyMap()
        )
        assertTrue(evidence.violations().joinToString(), evidence.violations().isEmpty())
    }

    @Test
    fun unreachableReviewNamesUnseenStateWithoutFabricatedObservation() {
        val evidence = UiReviewEvidence(
            status = TerminalStatus.NOT_RUN,
            reasonCode = "DEVICE_LOCKED",
            artifacts = setOf("lockscreen.xml", "lockscreen.png"),
            observations = listOf(UiObservation("Device keyguard is visible", setOf("lockscreen.xml", "lockscreen.png"))),
            actions = emptyList(),
            unseenStates = mapOf("dashboard" to "DEVICE_LOCKED")
        )
        assertTrue(evidence.violations().joinToString(), evidence.violations().isEmpty())
    }

    @Test
    fun missingEvidenceAndClaimsAboutUnseenUiAreRejected() {
        val evidence = UiReviewEvidence(
            status = TerminalStatus.NOT_RUN,
            reasonCode = "DEVICE_LOCKED",
            artifacts = setOf("lockscreen.xml"),
            observations = listOf(UiObservation("Dashboard is well aligned", emptySet())),
            actions = listOf(UiActionEvidence("tap", setOf("lockscreen.xml"), setOf("lockscreen.xml"))),
            unseenStates = mapOf("dashboard" to "DEVICE_LOCKED")
        )
        val violations = evidence.violations()
        assertEquals(3, violations.size)
        assertTrue(violations.any { it.contains("evidence is missing") })
        assertTrue(violations.any { it.contains("before and after") })
        assertTrue(violations.any { it.contains("described as observed") })
    }
}

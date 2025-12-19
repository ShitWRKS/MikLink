/*
 * Purpose: Define deterministic ordering, visibility, and expandability rules for test sections in execution UI.
 * Inputs: Lists of TestSectionSnapshot instances with ids/status; no side effects or UI dependencies.
 * Outputs: Ordered/filtered lists and predicates used by presentation-layer composables.
 */
package com.app.miklink.ui.test

import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.core.domain.test.model.TestSectionStatus

object TestSectionDisplayPolicy {
    private val orderedIds = listOf(
        TestSectionId.NETWORK,
        TestSectionId.NEIGHBORS,
        TestSectionId.LINK,
        TestSectionId.PING,
        TestSectionId.TDR,
        TestSectionId.SPEED
    )

    private val finalStatuses = setOf(
        TestSectionStatus.PASS,
        TestSectionStatus.FAIL,
        TestSectionStatus.SKIP,
        TestSectionStatus.INFO
    )

    fun ordered(sections: List<TestSectionSnapshot>): List<TestSectionSnapshot> =
        sections.sortedBy { orderedIds.indexOf(it.id).takeIf { idx -> idx >= 0 } ?: Int.MAX_VALUE }

    /**
     * Progressive reveal: keep all final sections plus only the first pending/running section.
     */
    fun visibleForRunning(sections: List<TestSectionSnapshot>): List<TestSectionSnapshot> {
        val visible = mutableListOf<TestSectionSnapshot>()
        var pendingIncluded = false
        sections.forEach { section ->
            val isFinal = section.status in finalStatuses
            val isPending = section.status == TestSectionStatus.PENDING || section.status == TestSectionStatus.RUNNING
            if (isFinal || (isPending && !pendingIncluded)) {
                visible += section
            }
            if (isPending && !pendingIncluded) pendingIncluded = true
        }
        return visible
    }

    fun isExpandable(status: TestSectionStatus): Boolean = status in finalStatuses
}

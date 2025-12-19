/*
 * Purpose: Typed snapshot contract for test execution shared between engine and UI.
 * Inputs: Section identifiers/status, typed payloads, and progress key.
 * Outputs: Immutable snapshot instances consumed by renderers and history mappers.
 * Notes: Enforces a single typed pipeline (ADR-0011), replacing stringly maps.
 */
package com.app.miklink.core.domain.test.model

import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.PingSample
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.model.report.TdrEntry

enum class TestSectionId {
    NETWORK,
    LINK,
    TDR,
    NEIGHBORS,
    PING,
    SPEED,
    SAVING_REPORT,
    REPORT,
    UNKNOWN
}

enum class TestSectionStatus {
    PENDING,
    RUNNING,
    PASS,
    FAIL,
    SKIP,
    INFO
}

enum class TestProgressKey {
    PREPARING,
    NETWORK_CONFIG,
    LINK,
    TDR,
    NEIGHBORS,
    PING,
    SPEED,
    SAVING_REPORT,
    COMPLETED,
    ERROR
}

sealed interface TestSectionPayload {
    data object None : TestSectionPayload
    data class Network(
        val mode: String? = null,
        val address: String? = null,
        val gateway: String? = null,
        val dns: String? = null,
        val message: String? = null
    ) : TestSectionPayload
    data class Link(val data: LinkStatusData) : TestSectionPayload
    data class Tdr(val entries: List<TdrEntry>) : TestSectionPayload
    data class Neighbors(val entries: List<NeighborData>) : TestSectionPayload
    data class Ping(val samples: List<PingSample>) : TestSectionPayload
    data class Speed(val data: SpeedTestData) : TestSectionPayload
}

data class TestSectionSnapshot(
    val id: TestSectionId,
    val status: TestSectionStatus,
    val payload: TestSectionPayload = TestSectionPayload.None,
    val title: String? = null,
    val warning: String? = null
)

data class TestRunSnapshot(
    val sections: List<TestSectionSnapshot> = emptyList(),
    val progress: TestProgressKey = TestProgressKey.PREPARING,
    val percent: Int = 0,
    val notes: String? = null
)

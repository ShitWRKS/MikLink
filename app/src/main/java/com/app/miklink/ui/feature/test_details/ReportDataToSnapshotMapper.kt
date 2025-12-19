/*
 * Purpose: Map persisted ReportData (v1 compatible) into typed TestRunSnapshot for unified rendering.
 * Inputs: ReportData decoded via ReportResultsCodec.
 * Outputs: TestRunSnapshot with section statuses/payloads set based on available data.
 * Notes: Handles missing fields gracefully for old reports; used by history/PDF pipelines.
 */
package com.app.miklink.ui.feature.test_details

import com.app.miklink.core.domain.model.report.ReportData
import com.app.miklink.core.domain.test.model.TestProgressKey
import com.app.miklink.core.domain.test.model.TestRunSnapshot
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionPayload
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.core.domain.test.model.TestSectionStatus

class ReportDataToSnapshotMapper {

    fun map(reportData: ReportData): TestRunSnapshot {
        val sections = mutableListOf<TestSectionSnapshot>()

        sections += mapNetwork(reportData)
        sections += mapLink(reportData)
        sections += mapTdr(reportData)
        sections += mapNeighbors(reportData)
        sections += mapPing(reportData)
        sections += mapSpeed(reportData)

        return TestRunSnapshot(
            sections = sections,
            progress = TestProgressKey.COMPLETED,
            percent = 100
        )
    }

    private fun mapNetwork(reportData: ReportData): TestSectionSnapshot {
        val payload = reportData.network?.let {
            TestSectionPayload.Network(
                mode = it.mode,
                address = it.address,
                gateway = it.gateway,
                dns = it.dns,
                message = it.message
            )
        } ?: TestSectionPayload.None
        val status = if (reportData.network != null) TestSectionStatus.PASS else TestSectionStatus.INFO
        return TestSectionSnapshot(
            id = TestSectionId.NETWORK,
            status = status,
            payload = payload,
            title = "Network"
        )
    }

    private fun mapLink(reportData: ReportData): TestSectionSnapshot {
        val payload = reportData.linkStatus?.let { TestSectionPayload.Link(it) } ?: TestSectionPayload.None
        val status = if (reportData.linkStatus != null) TestSectionStatus.PASS else TestSectionStatus.INFO
        return TestSectionSnapshot(
            id = TestSectionId.LINK,
            status = status,
            payload = payload,
            title = "Link"
        )
    }

    private fun mapTdr(reportData: ReportData): TestSectionSnapshot {
        val payload = if (reportData.tdr.isNotEmpty()) {
            TestSectionPayload.Tdr(reportData.tdr)
        } else {
            TestSectionPayload.None
        }
        val status = if (reportData.tdr.isNotEmpty()) TestSectionStatus.PASS else TestSectionStatus.INFO
        return TestSectionSnapshot(
            id = TestSectionId.TDR,
            status = status,
            payload = payload,
            title = "TDR"
        )
    }

    private fun mapNeighbors(reportData: ReportData): TestSectionSnapshot {
        val payload = if (reportData.neighbors.isNotEmpty()) {
            TestSectionPayload.Neighbors(reportData.neighbors)
        } else {
            TestSectionPayload.None
        }
        val status = if (reportData.neighbors.isNotEmpty()) TestSectionStatus.PASS else TestSectionStatus.INFO
        return TestSectionSnapshot(
            id = TestSectionId.NEIGHBORS,
            status = status,
            payload = payload,
            title = "LLDP/CDP"
        )
    }

    private fun mapPing(reportData: ReportData): TestSectionSnapshot {
        val payload = if (reportData.pingSamples.isNotEmpty()) {
            TestSectionPayload.Ping(reportData.pingSamples)
        } else {
            TestSectionPayload.None
        }
        val status = if (reportData.pingSamples.isNotEmpty()) TestSectionStatus.PASS else TestSectionStatus.INFO
        return TestSectionSnapshot(
            id = TestSectionId.PING,
            status = status,
            payload = payload,
            title = "Ping"
        )
    }

    private fun mapSpeed(reportData: ReportData): TestSectionSnapshot {
        val payload = reportData.speedTest?.let { TestSectionPayload.Speed(it) } ?: TestSectionPayload.None
        val status = if (reportData.speedTest != null) TestSectionStatus.PASS else TestSectionStatus.INFO
        return TestSectionSnapshot(
            id = TestSectionId.SPEED,
            status = status,
            payload = payload,
            title = "Speed Test"
        )
    }
}

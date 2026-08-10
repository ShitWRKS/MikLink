package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.NetworkData
import com.app.miklink.core.domain.model.report.PingSample
import com.app.miklink.core.domain.model.report.ReportData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.model.report.TdrEntry
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionPayload
import com.app.miklink.core.domain.test.model.TestSectionStatus
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.ui.feature.test_details.ReportDataToSnapshotMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultPresentationScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("result-presentation")

    @Test
    fun everyTypedResultSectionMapsToVisibleReadySnapshotData() {
        assertCatalogMembership("result-presentation", FeatureGroup.RESULT_PRESENTATION)
        val report = ReportData(
            network = NetworkData(mode = "DHCP", address = "192.0.2.2/24", gateway = "192.0.2.1", dns = "192.0.2.53"),
            linkStatus = LinkStatusData(status = "link-ok", rate = "1G"),
            tdr = listOf(TdrEntry(distance = "12m", status = "ok", description = "pair-a")),
            neighbors = listOf(NeighborData(identity = "switch", interfaceName = "ether1", discoveredBy = "lldp")),
            pingSamples = listOf(PingSample(target = "192.0.2.1", avgRtt = "1ms", packetLoss = "0%")),
            speedTest = SpeedTestData(status = "done", ping = "2ms", tcpDownload = "100Mbps", tcpUpload = "50Mbps")
        )
        val snapshot = ReportDataToSnapshotMapper().map(report)

        assertEquals(
            setOf(
                TestSectionId.NETWORK,
                TestSectionId.LINK,
                TestSectionId.TDR,
                TestSectionId.NEIGHBORS,
                TestSectionId.PING,
                TestSectionId.SPEED
            ),
            snapshot.sections.map { it.id }.toSet()
        )
        assertTrue(snapshot.sections.all { it.status == TestSectionStatus.PASS })
        assertTrue(snapshot.sections.none { it.payload == TestSectionPayload.None })
        assertEquals(100, snapshot.percent)
    }
}

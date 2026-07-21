package com.app.miklink.core.domain.policy

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestThresholds
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.TestSectionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TestQualityPolicyTest {
    private val policy = TestQualityPolicy()

    @Test
    fun `link rate null blank or invalid fails when threshold active`() {
        listOf<String?>(null, "", "not-a-rate").forEach { raw ->
            assertEquals(
                "rate=$raw",
                TestSectionStatus.FAIL,
                policy.evaluateLink(LinkStatusData("up", raw), profile(), client()).status
            )
        }
    }

    @Test
    fun `invalid link threshold fails closed`() {
        val profile = profile(TestThresholds.defaults().copy(linkMinRate = "broken"))

        assertEquals(
            TestSectionStatus.FAIL,
            policy.evaluateLink(LinkStatusData("up", "1G"), profile, client()).status
        )
    }

    @Test
    fun `valid link rate is compared above and below threshold`() {
        assertEquals(TestSectionStatus.PASS, policy.evaluateLink(LinkStatusData("up", "1G"), profile(), client()).status)
        assertEquals(TestSectionStatus.FAIL, policy.evaluateLink(LinkStatusData("up", "100Mbps"), profile(), client()).status)
    }

    @Test
    fun `download null or invalid fails`() {
        listOf<String?>(null, "corrupt 900").forEach { raw ->
            assertEquals(
                TestSectionStatus.FAIL,
                policy.evaluateSpeed(validSpeed().copy(tcpDownload = raw), profile()).status
            )
        }
    }

    @Test
    fun `upload null or invalid fails`() {
        listOf<String?>(null, "900 trailing-junk").forEach { raw ->
            assertEquals(
                TestSectionStatus.FAIL,
                policy.evaluateSpeed(validSpeed().copy(tcpUpload = raw), profile()).status
            )
        }
    }

    @Test
    fun `all required speed metrics fail when missing or invalid`() {
        assertEquals(TestSectionStatus.FAIL, policy.evaluateSpeed(validSpeed().copy(ping = null), profile()).status)
        assertEquals(TestSectionStatus.FAIL, policy.evaluateSpeed(validSpeed().copy(jitter = "bad"), profile()).status)
        assertEquals(TestSectionStatus.FAIL, policy.evaluateSpeed(validSpeed().copy(loss = "none"), profile()).status)
    }

    @Test
    fun `valid speed values pass and below threshold values fail`() {
        assertEquals(TestSectionStatus.PASS, policy.evaluateSpeed(validSpeed(), profile()).status)
        assertEquals(
            TestSectionStatus.FAIL,
            policy.evaluateSpeed(validSpeed().copy(tcpDownload = "49", tcpUpload = "49"), profile()).status
        )
    }

    @Test
    fun `ping non numeric loss fails`() {
        val outcome = validPing().copy(packetLoss = "zero percent")

        assertEquals(TestSectionStatus.FAIL, policy.evaluatePing(listOf(outcome), profile()).status)
    }

    @Test
    fun `ping non numeric RTT fails`() {
        val measurement = validMeasurement().copy(avgRtt = "corrupt", maxRtt = "also-corrupt")
        val outcome = validPing().copy(results = listOf(measurement))

        assertEquals(TestSectionStatus.FAIL, policy.evaluatePing(listOf(outcome), profile()).status)
    }

    @Test
    fun `ping response without usable metrics fails`() {
        val outcome = PingTargetOutcome(
            target = "8.8.8.8",
            resolved = "8.8.8.8",
            packetLoss = null,
            results = emptyList(),
            error = null
        )

        assertEquals(TestSectionStatus.FAIL, policy.evaluatePing(listOf(outcome), profile()).status)
    }

    @Test
    fun `valid ping above and below thresholds is evaluated`() {
        assertEquals(TestSectionStatus.PASS, policy.evaluatePing(listOf(validPing()), profile()).status)
        val slow = validPing().copy(results = listOf(validMeasurement().copy(avgRtt = "150ms", maxRtt = "250ms")))
        assertEquals(TestSectionStatus.FAIL, policy.evaluatePing(listOf(slow), profile()).status)
    }

    private fun validSpeed() = SpeedTestData(
        status = "ok",
        ping = "1ms/2ms/3ms",
        jitter = "1ms/2ms/3ms",
        loss = "0%",
        tcpDownload = "900Mbps",
        tcpUpload = "900Mbps"
    )

    private fun validPing() = PingTargetOutcome(
        target = "8.8.8.8",
        resolved = "8.8.8.8",
        packetLoss = "0%",
        results = listOf(validMeasurement()),
        error = null
    )

    private fun validMeasurement() = PingMeasurement(
        host = "8.8.8.8",
        minRtt = "8ms",
        avgRtt = "10ms",
        maxRtt = "12ms",
        packetLoss = "0%",
        sent = "4",
        received = "4",
        seq = "1",
        time = "10ms",
        ttl = "58",
        size = "64"
    )

    private fun profile(thresholds: TestThresholds = TestThresholds.defaults()) = TestProfile(
        profileId = 1,
        profileName = "Default",
        profileDescription = null,
        runTdr = true,
        runLinkStatus = true,
        runLldp = true,
        runPing = true,
        pingTarget1 = "8.8.8.8",
        pingTarget2 = null,
        pingTarget3 = null,
        pingCount = 4,
        runSpeedTest = true,
        thresholds = thresholds
    )

    private fun client() = Client(
        clientId = 1,
        companyName = "Acme",
        location = null,
        notes = null,
        networkMode = NetworkMode.DHCP,
        staticIp = null,
        staticSubnet = null,
        staticGateway = null,
        staticCidr = null,
        minLinkRate = "1G",
        socketPrefix = "",
        socketSuffix = "",
        socketSeparator = "",
        socketNumberPadding = 3,
        nextIdNumber = 1,
        speedTestServerAddress = null,
        speedTestServerUser = null,
        speedTestServerPassword = null
    )
}

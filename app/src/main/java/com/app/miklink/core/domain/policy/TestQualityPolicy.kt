/*
 * Purpose: Evaluate collected test measurements against user-configurable thresholds to decide PASS/FAIL per section.
 * Inputs: TestProfile (with thresholds), optional Client (for link minRate fallback), and raw measurement data (link, TDR, ping, speed).
 * Outputs: SectionEvaluation indicating status and optional warning reason, used by the test runner to tag sections.
 * Notes: Parsing is lenient (strips non-numeric chars); missing metrics do not trigger fail. Gateway policy defaults to FAIL on unresolved DHCP.
 */
package com.app.miklink.core.domain.policy

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.GatewayUnresolvedPolicy
import com.app.miklink.core.domain.model.PingThresholds
import com.app.miklink.core.domain.model.SpeedThresholds
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestThresholds
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.TestSectionStatus

data class SectionEvaluation(
    val status: TestSectionStatus,
    val warning: String? = null
)

class TestQualityPolicy(
    private val defaultThresholds: TestThresholds = TestThresholds.defaults()
) {

    fun evaluateLink(
        linkStatus: LinkStatusData,
        profile: TestProfile,
        client: Client
    ): SectionEvaluation {
        val thresholds = profile.thresholds ?: defaultThresholds
        val minRate = thresholds.linkMinRate ?: client.minLinkRate
        val status = linkStatus.status?.lowercase()
        if (status.isNullOrBlank() || status == "down" || status == "unknown") {
            return SectionEvaluation(TestSectionStatus.FAIL, "Link inattivo o sconosciuto")
        }
        if (!minRate.isNullOrBlank()) {
            val currentRate = parseRateMbps(linkStatus.rate)
            val requiredRate = parseRateMbps(minRate)
            if (currentRate != null && requiredRate != null && currentRate < requiredRate) {
                return SectionEvaluation(
                    TestSectionStatus.FAIL,
                    "Velocità link ${linkStatus.rate ?: "-"} sotto soglia $minRate"
                )
            }
        }
        return SectionEvaluation(TestSectionStatus.PASS)
    }

    fun evaluateTdr(
        summary: CableTestSummary,
        profile: TestProfile
    ): SectionEvaluation {
        val thresholds = profile.thresholds ?: defaultThresholds
        val failStatuses = thresholds.tdrFailStatuses
        val statusCandidates = buildList {
            summary.status?.let { add(it) }
            summary.entries.forEach { entry ->
                entry.status?.let { add(it) }
                entry.description?.let { add(it) }
            }
        }
        val failing = statusCandidates.firstOrNull { value ->
            failStatuses.contains(value.lowercase())
        }
        return if (failing != null) {
            SectionEvaluation(TestSectionStatus.FAIL, "TDR rileva stato critico: $failing")
        } else {
            SectionEvaluation(TestSectionStatus.PASS)
        }
    }

    fun evaluatePing(
        outcomes: List<PingTargetOutcome>,
        profile: TestProfile
    ): SectionEvaluation {
        val thresholds = profile.thresholds ?: defaultThresholds
        val failReasons = mutableListOf<String>()

        outcomes.forEach { outcome ->
            val targetLabel = outcome.resolved ?: outcome.target
            val isGateway = outcome.target.equals("DHCP_GATEWAY", ignoreCase = true)
            if (isGateway && outcome.resolved == null && thresholds.gatewayPolicy == GatewayUnresolvedPolicy.FAIL) {
                failReasons += "Gateway DHCP non risolvibile"
                return@forEach
            }
            val targetThreshold = if (isLocalTarget(targetLabel)) thresholds.pingLocal else thresholds.pingExternal
            val loss = parsePercent(outcome.packetLoss) ?: parsePercentFromResults(outcome.results)
            if (loss != null && loss > targetThreshold.maxLossPercent) {
                failReasons += "Ping $targetLabel loss ${formatNumber(loss)}% sopra soglia ${targetThreshold.maxLossPercent}%"
            }
            val avgRtt = extractAvgRtt(outcome.results)
            if (avgRtt != null && avgRtt > targetThreshold.maxAvgRttMs) {
                failReasons += "Ping $targetLabel avg ${formatNumber(avgRtt)}ms sopra soglia ${targetThreshold.maxAvgRttMs}ms"
            }
            val maxRtt = extractMaxRtt(outcome.results)
            if (maxRtt != null && maxRtt > targetThreshold.maxRttMs) {
                failReasons += "Ping $targetLabel max ${formatNumber(maxRtt)}ms sopra soglia ${targetThreshold.maxRttMs}ms"
            }
            if (outcome.error != null && outcome.results.isEmpty()) {
                failReasons += "Ping $targetLabel errore: ${outcome.error}"
            }
        }

        return if (failReasons.isNotEmpty()) {
            SectionEvaluation(TestSectionStatus.FAIL, failReasons.joinToString("; "))
        } else {
            SectionEvaluation(TestSectionStatus.PASS)
        }
    }

    fun evaluateSpeed(
        speed: SpeedTestData,
        profile: TestProfile
    ): SectionEvaluation {
        val thresholds = profile.thresholds ?: defaultThresholds
        val failReasons = mutableListOf<String>()

        val speedPing = takeLeadingNumber(speed.ping)
        if (speedPing != null && speedPing > thresholds.speed.maxPingMs) {
            failReasons += "SpeedTest ping ${formatNumber(speedPing)}ms sopra soglia ${thresholds.speed.maxPingMs}ms"
        }

        val jitter = takeLeadingNumber(speed.jitter)
        if (jitter != null && jitter > thresholds.speed.maxJitterMs) {
            failReasons += "SpeedTest jitter ${formatNumber(jitter)}ms sopra soglia ${thresholds.speed.maxJitterMs}ms"
        }

        val loss = parsePercent(speed.loss)
        if (loss != null && loss > thresholds.speed.maxLossPercent) {
            failReasons += "SpeedTest loss ${formatNumber(loss)}% sopra soglia ${thresholds.speed.maxLossPercent}%"
        }

        val download = takeLeadingNumber(speed.tcpDownload)
        if (download != null && download < thresholds.speed.minDownloadMbps) {
            failReasons += "Download ${formatNumber(download)}Mbps sotto soglia ${thresholds.speed.minDownloadMbps}Mbps"
        }

        val upload = takeLeadingNumber(speed.tcpUpload)
        if (upload != null && upload < thresholds.speed.minUploadMbps) {
            failReasons += "Upload ${formatNumber(upload)}Mbps sotto soglia ${thresholds.speed.minUploadMbps}Mbps"
        }

        return if (failReasons.isNotEmpty()) {
            SectionEvaluation(TestSectionStatus.FAIL, failReasons.joinToString("; "))
        } else {
            SectionEvaluation(TestSectionStatus.PASS)
        }
    }

    private fun isLocalTarget(target: String?): Boolean {
        if (target.isNullOrBlank()) return false
        val normalized = target.lowercase()
        if (normalized == "dhcp_gateway") return true
        if (normalized.startsWith("10.") || normalized.startsWith("192.168.")) return true
        if (normalized.startsWith("172.")) {
            val second = normalized.removePrefix("172.").substringBefore('.').toIntOrNull()
            if (second != null && second in 16..31) return true
        }
        return false
    }

    private fun parsePercent(raw: String?): Double? {
        raw ?: return null
        val digits = raw.trim().takeWhile { it.isDigit() || it == '.' }
        return digits.toDoubleOrNull()
    }

    private fun parsePercentFromResults(results: List<PingMeasurement>): Double? =
        results.lastOrNull()?.packetLoss?.let { parsePercent(it) }

    private fun extractAvgRtt(results: List<PingMeasurement>): Double? =
        results.mapNotNull { takeLeadingNumber(it.avgRtt) }.averageOrNull()

    private fun extractMaxRtt(results: List<PingMeasurement>): Double? =
        results.mapNotNull { takeLeadingNumber(it.maxRtt) }.maxOrNull()

    private fun takeLeadingNumber(raw: String?): Double? {
        raw ?: return null
        val trimmed = raw.trim().lowercase()
        val regex = Regex("""(\d+(?:\.\d+)?)(ms|us|s)?""")
        val match = regex.find(trimmed) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        return when (match.groupValues.getOrNull(2)) {
            "us" -> value / 1000.0
            "s" -> value * 1000.0
            "ms", "" -> value
            else -> value
        }
    }

    private fun parseRateMbps(raw: String?): Double? {
        raw ?: return null
        val number = takeLeadingNumber(raw) ?: return null
        val lower = raw.lowercase()
        return when {
            lower.contains("g") -> number * 1000
            else -> number
        }
    }

    private fun Iterable<Double>.averageOrNull(): Double? {
        if (!this.iterator().hasNext()) return null
        return this.average()
    }

    private fun formatNumber(value: Double): String = "%.1f".format(value)
}

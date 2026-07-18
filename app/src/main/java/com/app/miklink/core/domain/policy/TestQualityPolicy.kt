/*
 * Purpose: Evaluate collected test measurements against user-configurable thresholds to decide PASS/FAIL per section.
 * Inputs: TestProfile (with thresholds), optional Client (for link minRate fallback), and raw measurement data (link, TDR, ping, speed).
 * Outputs: SectionEvaluation indicating status and optional warning reason, used by the test runner to tag sections.
 * Notes: Parsing is lenient (strips non-numeric chars); missing metrics do not trigger fail. Gateway policy defaults to FAIL on unresolved DHCP.
 */
package com.app.miklink.core.domain.policy

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.GatewayUnresolvedPolicy
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
    private val defaultThresholds: TestThresholds = TestThresholds.defaults(),
    private val thresholdEvaluationObserver: ((testName: String, fields: Map<String, Any?>) -> Unit)? = null
) {

    fun evaluateLink(
        linkStatus: LinkStatusData,
        profile: TestProfile,
        client: Client
    ): SectionEvaluation {
        val thresholds = profile.thresholds ?: defaultThresholds
        val minRate = thresholds.linkMinRate ?: client.minLinkRate
        val status = linkStatus.status?.lowercase()
        val currentRate = parseRateMbps(linkStatus.rate)
        val requiredRate = if (minRate.isNullOrBlank()) null else parseRateMbps(minRate)
        val input = mapOf(
            "status" to linkStatus.status,
            "rate" to linkStatus.rate,
            "rateMbps" to currentRate
        )
        val thresholdFields = mapOf(
            "linkMinRate" to minRate,
            "linkMinRateMbps" to requiredRate
        )
        if (status.isNullOrBlank() || status == "down" || status == "unknown") {
            val decision = SectionEvaluation(TestSectionStatus.FAIL, "Link inattivo o sconosciuto")
            notifyThresholdEvaluation("link", input, thresholdFields, decision)
            return decision
        }
        if (!minRate.isNullOrBlank() && currentRate != null && requiredRate != null && currentRate < requiredRate) {
            val decision = SectionEvaluation(
                TestSectionStatus.FAIL,
                "Velocita link ${linkStatus.rate ?: "-"} sotto soglia $minRate"
            )
            notifyThresholdEvaluation("link", input, thresholdFields, decision)
            return decision
        }
        val decision = SectionEvaluation(TestSectionStatus.PASS)
        notifyThresholdEvaluation("link", input, thresholdFields, decision)
        return decision
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
        val decision = if (failing != null) {
            SectionEvaluation(TestSectionStatus.FAIL, "TDR rileva stato critico: $failing")
        } else {
            SectionEvaluation(TestSectionStatus.PASS)
        }
        notifyThresholdEvaluation(
            "tdr",
            input = mapOf("summaryStatus" to summary.status, "entries" to summary.entries),
            thresholds = mapOf("tdrFailStatuses" to failStatuses),
            decision = decision
        )
        return decision
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

        val decision = if (failReasons.isNotEmpty()) {
            SectionEvaluation(TestSectionStatus.FAIL, failReasons.joinToString("; "))
        } else {
            SectionEvaluation(TestSectionStatus.PASS)
        }
        notifyThresholdEvaluation(
            "ping",
            input = mapOf("outcomes" to outcomes),
            thresholds = mapOf(
                "pingLocal" to thresholds.pingLocal,
                "pingExternal" to thresholds.pingExternal,
                "gatewayPolicy" to thresholds.gatewayPolicy.name
            ),
            decision = decision
        )
        return decision
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

        val decision = if (failReasons.isNotEmpty()) {
            SectionEvaluation(TestSectionStatus.FAIL, failReasons.joinToString("; "))
        } else {
            SectionEvaluation(TestSectionStatus.PASS)
        }
        notifyThresholdEvaluation(
            "speed",
            input = mapOf(
                "ping" to speed.ping,
                "jitter" to speed.jitter,
                "loss" to speed.loss,
                "tcpDownload" to speed.tcpDownload,
                "tcpUpload" to speed.tcpUpload
            ),
            thresholds = mapOf(
                "maxPingMs" to thresholds.speed.maxPingMs,
                "maxJitterMs" to thresholds.speed.maxJitterMs,
                "maxLossPercent" to thresholds.speed.maxLossPercent,
                "minDownloadMbps" to thresholds.speed.minDownloadMbps,
                "minUploadMbps" to thresholds.speed.minUploadMbps
            ),
            decision = decision
        )
        return decision
    }

    private fun notifyThresholdEvaluation(
        testName: String,
        input: Map<String, Any?>,
        thresholds: Map<String, Any?>,
        decision: SectionEvaluation
    ) {
        thresholdEvaluationObserver?.invoke(
            testName,
            mapOf(
                "input" to input,
                "thresholds" to thresholds,
                "status" to decision.status.name,
                "reason" to decision.warning
            )
        )
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

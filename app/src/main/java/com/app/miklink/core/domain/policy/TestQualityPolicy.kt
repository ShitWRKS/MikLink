/*
 * Purpose: Evaluate collected test measurements against user-configurable thresholds to decide PASS/FAIL per section.
 * Inputs: TestProfile (with thresholds), optional Client (for link minRate fallback), and raw measurement data (link, TDR, ping, speed).
 * Outputs: SectionEvaluation indicating status and optional warning reason, used by the test runner to tag sections.
 * Notes: Active thresholds are fail-closed: missing or malformed metrics produce a technical FAIL.
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
            "rateMbps" to currentRate.validValueOrNull()
        )
        val thresholdFields = mapOf(
            "linkMinRate" to minRate,
            "linkMinRateMbps" to requiredRate?.validValueOrNull()
        )
        if (status.isNullOrBlank() || status == "down" || status == "unknown") {
            val decision = SectionEvaluation(TestSectionStatus.FAIL, "Link inattivo o sconosciuto")
            notifyThresholdEvaluation("link", input, thresholdFields, decision)
            return decision
        }
        if (requiredRate != null) {
            val warning = when (requiredRate) {
                MetricValue.Missing -> "Soglia link mancante"
                is MetricValue.Invalid -> "Soglia link non valida: ${requiredRate.raw}"
                is MetricValue.Valid -> when (currentRate) {
                    MetricValue.Missing -> "Velocita link mancante"
                    is MetricValue.Invalid -> "Velocita link non valida: ${currentRate.raw}"
                    is MetricValue.Valid -> if (currentRate.value < requiredRate.value) {
                        "Velocita link ${linkStatus.rate ?: "-"} sotto soglia $minRate"
                    } else {
                        null
                    }
                }
            }
            if (warning != null) {
                val decision = SectionEvaluation(TestSectionStatus.FAIL, warning)
                notifyThresholdEvaluation("link", input, thresholdFields, decision)
                return decision
            }
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

        if (outcomes.isEmpty()) {
            failReasons += "Nessun risultato ping disponibile"
        }

        outcomes.forEach { outcome ->
            val targetLabel = outcome.resolved ?: outcome.target
            val isGateway = outcome.target.equals("DHCP_GATEWAY", ignoreCase = true)
            if (isGateway && outcome.resolved == null && thresholds.gatewayPolicy == GatewayUnresolvedPolicy.FAIL) {
                failReasons += "Gateway DHCP non risolvibile"
                return@forEach
            }
            val targetThreshold = if (isLocalTarget(targetLabel)) thresholds.pingLocal else thresholds.pingExternal
            val loss = parsePingLoss(outcome)
            compareRequiredMetric(loss, "Ping $targetLabel loss", failReasons) { value ->
                if (value > targetThreshold.maxLossPercent) {
                    "Ping $targetLabel loss ${formatNumber(value)}% sopra soglia ${targetThreshold.maxLossPercent}%"
                } else null
            }
            val avgRtt = aggregateRtt(outcome.results) { it.avgRtt }
            compareRequiredMetric(avgRtt, "Ping $targetLabel RTT medio", failReasons) { value ->
                if (value > targetThreshold.maxAvgRttMs) {
                    "Ping $targetLabel avg ${formatNumber(value)}ms sopra soglia ${targetThreshold.maxAvgRttMs}ms"
                } else null
            }
            val maxRtt = maxRtt(outcome.results)
            compareRequiredMetric(maxRtt, "Ping $targetLabel RTT massimo", failReasons) { value ->
                if (value > targetThreshold.maxRttMs) {
                    "Ping $targetLabel max ${formatNumber(value)}ms sopra soglia ${targetThreshold.maxRttMs}ms"
                } else null
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

        compareRequiredMetric(parseDuration(speed.ping), "SpeedTest ping", failReasons) { value ->
            if (value > thresholds.speed.maxPingMs) "SpeedTest ping ${formatNumber(value)}ms sopra soglia ${thresholds.speed.maxPingMs}ms" else null
        }
        compareRequiredMetric(parseDuration(speed.jitter), "SpeedTest jitter", failReasons) { value ->
            if (value > thresholds.speed.maxJitterMs) "SpeedTest jitter ${formatNumber(value)}ms sopra soglia ${thresholds.speed.maxJitterMs}ms" else null
        }
        compareRequiredMetric(parsePercent(speed.loss), "SpeedTest loss", failReasons) { value ->
            if (value > thresholds.speed.maxLossPercent) "SpeedTest loss ${formatNumber(value)}% sopra soglia ${thresholds.speed.maxLossPercent}%" else null
        }
        compareRequiredMetric(parseBandwidth(speed.tcpDownload), "Download", failReasons) { value ->
            if (value < thresholds.speed.minDownloadMbps) "Download ${formatNumber(value)}Mbps sotto soglia ${thresholds.speed.minDownloadMbps}Mbps" else null
        }
        compareRequiredMetric(parseBandwidth(speed.tcpUpload), "Upload", failReasons) { value ->
            if (value < thresholds.speed.minUploadMbps) "Upload ${formatNumber(value)}Mbps sotto soglia ${thresholds.speed.minUploadMbps}Mbps" else null
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

    private fun parsePercent(raw: String?): MetricValue = parseScalar(raw, PERCENT_PATTERN)

    private fun parseBandwidth(raw: String?): MetricValue = parseScalar(raw, BANDWIDTH_PATTERN)

    private fun parseDuration(raw: String?): MetricValue {
        if (raw.isNullOrBlank()) return MetricValue.Missing
        val values = raw.trim().split('/').map { part -> parseScalar(part, DURATION_PATTERN) }
        val invalid = values.firstOrNull { it !is MetricValue.Valid }
        return invalid ?: values.first()
    }

    private fun parseRateMbps(raw: String?): MetricValue {
        if (raw.isNullOrBlank()) return MetricValue.Missing
        val match = RATE_PATTERN.matchEntire(raw.trim()) ?: return MetricValue.Invalid(raw)
        val value = match.groupValues[1].toDoubleOrNull() ?: return MetricValue.Invalid(raw)
        return MetricValue.Valid(if (match.groupValues[2].startsWith("g", true)) value * 1000 else value)
    }

    private fun parseScalar(raw: String?, pattern: Regex): MetricValue {
        if (raw.isNullOrBlank()) return MetricValue.Missing
        val match = pattern.matchEntire(raw.trim()) ?: return MetricValue.Invalid(raw)
        val value = match.groupValues[1].toDoubleOrNull() ?: return MetricValue.Invalid(raw)
        val unit = match.groupValues.getOrElse(2) { "" }.lowercase()
        return MetricValue.Valid(
            when (unit) {
                "us" -> value / 1000.0
                "s" -> value * 1000.0
                else -> value
            }
        )
    }

    private fun parsePingLoss(outcome: PingTargetOutcome): MetricValue =
        if (!outcome.packetLoss.isNullOrBlank()) {
            parsePercent(outcome.packetLoss)
        } else {
            parsePercent(outcome.results.lastOrNull()?.packetLoss)
        }

    private fun aggregateRtt(
        results: List<PingMeasurement>,
        selector: (PingMeasurement) -> String?
    ): MetricValue {
        if (results.isEmpty()) return MetricValue.Missing
        val values = results.map { parseDuration(selector(it)) }
        val invalid = values.firstOrNull { it !is MetricValue.Valid }
        if (invalid != null) return invalid
        return MetricValue.Valid(values.filterIsInstance<MetricValue.Valid>().map { it.value }.average())
    }

    private fun maxRtt(results: List<PingMeasurement>): MetricValue {
        if (results.isEmpty()) return MetricValue.Missing
        val values = results.map { parseDuration(it.maxRtt) }
        val invalid = values.firstOrNull { it !is MetricValue.Valid }
        if (invalid != null) return invalid
        return MetricValue.Valid(values.filterIsInstance<MetricValue.Valid>().maxOf { it.value })
    }

    private fun compareRequiredMetric(
        metric: MetricValue,
        label: String,
        failures: MutableList<String>,
        compare: (Double) -> String?
    ) {
        when (metric) {
            MetricValue.Missing -> failures += "$label mancante"
            is MetricValue.Invalid -> failures += "$label non valido: ${metric.raw}"
            is MetricValue.Valid -> compare(metric.value)?.let { failures += it }
        }
    }

    private fun MetricValue.validValueOrNull(): Double? = (this as? MetricValue.Valid)?.value

    private fun formatNumber(value: Double): String = "%.1f".format(value)
}

private sealed interface MetricValue {
    data class Valid(val value: Double) : MetricValue
    data object Missing : MetricValue
    data class Invalid(val raw: String) : MetricValue
}

private val PERCENT_PATTERN = Regex("""^(\d+(?:\.\d+)?)\s*(%)?$""", RegexOption.IGNORE_CASE)
private val BANDWIDTH_PATTERN = Regex("""^(\d+(?:\.\d+)?)\s*(mbps)?$""", RegexOption.IGNORE_CASE)
private val DURATION_PATTERN = Regex("""^(\d+(?:\.\d+)?)\s*(ms|us|s)?$""", RegexOption.IGNORE_CASE)
private val RATE_PATTERN = Regex("""^(\d+(?:\.\d+)?)\s*(g|gbps|gbit/s|m|mbps|mbit/s)?$""", RegexOption.IGNORE_CASE)

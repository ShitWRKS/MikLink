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
import com.app.miklink.core.domain.test.TestRunTextProvider
import com.app.miklink.core.domain.validation.StrictLinkRateParser

data class SectionEvaluation(
    val status: TestSectionStatus,
    val warning: String? = null
)

class TestQualityPolicy(
    private val textProvider: TestRunTextProvider,
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
            val decision = SectionEvaluation(TestSectionStatus.FAIL, textProvider.qualityLinkInactive())
            notifyThresholdEvaluation("link", input, thresholdFields, decision)
            return decision
        }
        if (requiredRate != null) {
            val warning = when (requiredRate) {
                MetricValue.Missing -> textProvider.qualityLinkThresholdMissing()
                is MetricValue.Invalid -> textProvider.qualityLinkThresholdInvalid(requiredRate.raw)
                is MetricValue.Valid -> when (currentRate) {
                    MetricValue.Missing -> textProvider.qualityLinkSpeedMissing()
                    is MetricValue.Invalid -> textProvider.qualityLinkSpeedInvalid(currentRate.raw)
                    is MetricValue.Valid -> if (currentRate.value < requiredRate.value) {
                        textProvider.qualityLinkBelowThreshold(linkStatus.rate ?: "-", minRate ?: "-")
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
            SectionEvaluation(TestSectionStatus.FAIL, textProvider.qualityTdrCritical(failing))
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
            failReasons += textProvider.qualityPingNoResults()
        }

        outcomes.forEach { outcome ->
            val targetLabel = outcome.resolved ?: outcome.target
            val isGateway = outcome.target.equals("DHCP_GATEWAY", ignoreCase = true)
            if (isGateway && outcome.resolved == null && thresholds.gatewayPolicy == GatewayUnresolvedPolicy.FAIL) {
                failReasons += textProvider.qualityGatewayUnresolved()
                return@forEach
            }
            val targetThreshold = if (isLocalTarget(targetLabel)) thresholds.pingLocal else thresholds.pingExternal
            val loss = parsePingLoss(outcome)
            compareRequiredMetric(loss, textProvider.qualityPingLossLabel(targetLabel), failReasons) { value ->
                if (value > targetThreshold.maxLossPercent) {
                    textProvider.qualityPingLossAbove(
                        targetLabel,
                        formatNumber(value),
                        formatNumber(targetThreshold.maxLossPercent)
                    )
                } else null
            }
            val avgRtt = aggregateRtt(outcome.results) { it.avgRtt }
            compareRequiredMetric(avgRtt, textProvider.qualityPingAverageRttLabel(targetLabel), failReasons) { value ->
                if (value > targetThreshold.maxAvgRttMs) {
                    textProvider.qualityPingAverageRttAbove(
                        targetLabel,
                        formatNumber(value),
                        formatNumber(targetThreshold.maxAvgRttMs)
                    )
                } else null
            }
            val maxRtt = maxRtt(outcome.results)
            compareRequiredMetric(maxRtt, textProvider.qualityPingMaximumRttLabel(targetLabel), failReasons) { value ->
                if (value > targetThreshold.maxRttMs) {
                    textProvider.qualityPingMaximumRttAbove(
                        targetLabel,
                        formatNumber(value),
                        formatNumber(targetThreshold.maxRttMs)
                    )
                } else null
            }
            if (outcome.error != null && outcome.results.isEmpty()) {
                failReasons += textProvider.qualityPingError(targetLabel, outcome.error)
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

        compareRequiredMetric(parseDuration(speed.ping), textProvider.qualitySpeedPingLabel(), failReasons) { value ->
            if (value > thresholds.speed.maxPingMs) {
                textProvider.qualityMetricAboveThreshold(
                    textProvider.qualitySpeedPingLabel(), formatNumber(value), " ms", formatNumber(thresholds.speed.maxPingMs)
                )
            } else null
        }
        compareRequiredMetric(parseDuration(speed.jitter), textProvider.qualitySpeedJitterLabel(), failReasons) { value ->
            if (value > thresholds.speed.maxJitterMs) {
                textProvider.qualityMetricAboveThreshold(
                    textProvider.qualitySpeedJitterLabel(), formatNumber(value), " ms", formatNumber(thresholds.speed.maxJitterMs)
                )
            } else null
        }
        compareRequiredMetric(parsePercent(speed.loss), textProvider.qualitySpeedLossLabel(), failReasons) { value ->
            if (value > thresholds.speed.maxLossPercent) {
                textProvider.qualityMetricAboveThreshold(
                    textProvider.qualitySpeedLossLabel(), formatNumber(value), "%", formatNumber(thresholds.speed.maxLossPercent)
                )
            } else null
        }
        compareRequiredMetric(parseBandwidth(speed.tcpDownload), textProvider.qualityDownloadLabel(), failReasons) { value ->
            if (value < thresholds.speed.minDownloadMbps) {
                textProvider.qualityMetricBelowThreshold(
                    textProvider.qualityDownloadLabel(), formatNumber(value), " Mbps", formatNumber(thresholds.speed.minDownloadMbps)
                )
            } else null
        }
        compareRequiredMetric(parseBandwidth(speed.tcpUpload), textProvider.qualityUploadLabel(), failReasons) { value ->
            if (value < thresholds.speed.minUploadMbps) {
                textProvider.qualityMetricBelowThreshold(
                    textProvider.qualityUploadLabel(), formatNumber(value), " Mbps", formatNumber(thresholds.speed.minUploadMbps)
                )
            } else null
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
        val value = StrictLinkRateParser.parseMbps(raw) ?: return MetricValue.Invalid(raw)
        return MetricValue.Valid(value)
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
            MetricValue.Missing -> failures += textProvider.qualityMetricMissing(label)
            is MetricValue.Invalid -> failures += textProvider.qualityMetricInvalid(label, metric.raw)
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

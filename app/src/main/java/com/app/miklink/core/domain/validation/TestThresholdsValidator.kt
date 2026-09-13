package com.app.miklink.core.domain.validation

import com.app.miklink.core.domain.model.TestThresholds

const val MAX_SPEED_THROUGHPUT_MBPS = 100_000.0

object StrictLinkRateParser {
    private val ratePattern = Regex(
        pattern = """^(\d+(?:\.\d+)?)\s*(g|gbps|gbit/s|m|mbps|mbit/s)?$""",
        option = RegexOption.IGNORE_CASE
    )

    fun parseMbps(raw: String): Double? {
        val match = ratePattern.matchEntire(raw.trim()) ?: return null
        val value = match.groupValues[1].toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null
        val valueMbps = if (match.groupValues[2].startsWith("g", ignoreCase = true)) value * 1000 else value
        return valueMbps.takeIf(Double::isFinite)
    }

    fun isValidOptional(raw: String?): Boolean =
        raw.isNullOrBlank() || parseMbps(raw) != null
}

object TestThresholdsValidator {
    fun isValidPercentage(value: Double): Boolean = value.isFinite() && value in 0.0..100.0

    fun isValidNonNegative(value: Double): Boolean = value.isFinite() && value >= 0.0

    fun isValidSpeedThroughput(value: Double): Boolean =
        value.isFinite() && value in 0.0..MAX_SPEED_THROUGHPUT_MBPS

    fun isValidPercentageInput(value: String): Boolean =
        value.isBlank() || value.toDoubleOrNull()?.let(::isValidPercentage) == true

    fun isValidNonNegativeInput(value: String): Boolean =
        value.isBlank() || value.toDoubleOrNull()?.let(::isValidNonNegative) == true

    fun isValidSpeedThroughputInput(value: String): Boolean =
        value.isBlank() || value.toDoubleOrNull()?.let(::isValidSpeedThroughput) == true

    fun validate(thresholds: TestThresholds) {
        require(StrictLinkRateParser.isValidOptional(thresholds.linkMinRate)) { "Invalid link minimum rate" }

        require(isValidPercentage(thresholds.pingLocal.maxLossPercent)) { "Invalid local ping loss percentage" }
        require(isValidNonNegative(thresholds.pingLocal.maxAvgRttMs)) { "Invalid local average RTT" }
        require(isValidNonNegative(thresholds.pingLocal.maxRttMs)) { "Invalid local maximum RTT" }

        require(isValidPercentage(thresholds.pingExternal.maxLossPercent)) { "Invalid external ping loss percentage" }
        require(isValidNonNegative(thresholds.pingExternal.maxAvgRttMs)) { "Invalid external average RTT" }
        require(isValidNonNegative(thresholds.pingExternal.maxRttMs)) { "Invalid external maximum RTT" }

        require(isValidNonNegative(thresholds.speed.maxPingMs)) { "Invalid speed test ping threshold" }
        require(isValidNonNegative(thresholds.speed.maxJitterMs)) { "Invalid speed test jitter threshold" }
        require(isValidPercentage(thresholds.speed.maxLossPercent)) { "Invalid speed test loss percentage" }
        require(isValidSpeedThroughput(thresholds.speed.minDownloadMbps)) { "Invalid minimum download threshold" }
        require(isValidSpeedThroughput(thresholds.speed.minUploadMbps)) { "Invalid minimum upload threshold" }
    }
}

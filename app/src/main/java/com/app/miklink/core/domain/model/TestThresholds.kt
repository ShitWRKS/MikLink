/*
 * Purpose: Hold configurable quality thresholds per test type (link, TDR, ping, speed) so pass/fail rules are data-driven.
 * Inputs: User-provided thresholds attached to a TestProfile; optional gateway policy for DHCP resolution failures.
 * Outputs: Immutable threshold values consumed by TestQualityPolicy to evaluate section status and warnings.
 * Notes: Defaults reflect conservative but practical values; keep numeric units documented in property names.
 */
package com.app.miklink.core.domain.model

data class PingThresholds(
    val maxLossPercent: Double,
    val maxAvgRttMs: Double,
    val maxRttMs: Double
)

data class SpeedThresholds(
    val maxPingMs: Double,
    val maxJitterMs: Double,
    val maxLossPercent: Double,
    val minDownloadMbps: Double,
    val minUploadMbps: Double
)

enum class GatewayUnresolvedPolicy {
    FAIL,
    SKIP
}

data class TestThresholds(
    val linkMinRate: String?,
    val tdrFailStatuses: Set<String>,
    val pingLocal: PingThresholds,
    val pingExternal: PingThresholds,
    val gatewayPolicy: GatewayUnresolvedPolicy,
    val speed: SpeedThresholds
) {
    companion object {
        fun defaults(): TestThresholds {
            return TestThresholds(
                linkMinRate = "1G",
                tdrFailStatuses = setOf("open", "short", "disconnected", "no-cable", "fail", "unknown"),
                pingLocal = PingThresholds(
                    maxLossPercent = 0.0,
                    maxAvgRttMs = 30.0,
                    maxRttMs = 50.0
                ),
                pingExternal = PingThresholds(
                    maxLossPercent = 1.0,
                    maxAvgRttMs = 120.0,
                    maxRttMs = 200.0
                ),
                gatewayPolicy = GatewayUnresolvedPolicy.FAIL,
                speed = SpeedThresholds(
                    maxPingMs = 50.0,
                    maxJitterMs = 20.0,
                    maxLossPercent = 1.0,
                    minDownloadMbps = 50.0,
                    minUploadMbps = 50.0
                )
            )
        }
    }
}

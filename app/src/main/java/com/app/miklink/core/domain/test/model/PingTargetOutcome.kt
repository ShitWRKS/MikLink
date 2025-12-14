package com.app.miklink.core.domain.test.model

data class PingTargetOutcome(
    val target: String,
    val resolved: String?,
    val packetLoss: String?,
    val results: List<PingMeasurement>,
    val error: String?
)

data class PingMeasurement(
    val host: String?,
    val minRtt: String?,
    val avgRtt: String?,
    val maxRtt: String?,
    val packetLoss: String?,
    val sent: String?,
    val received: String?,
    val seq: String?,
    val time: String?,
    val ttl: String?,
    val size: String?
)



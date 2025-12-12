package com.app.miklink.core.domain.test.model

import com.app.miklink.core.data.remote.mikrotik.dto.PingResult

data class PingTargetOutcome(
    val target: String,
    val resolved: String?,
    val packetLoss: String?,
    val results: List<PingResult>,
    val error: String?
)



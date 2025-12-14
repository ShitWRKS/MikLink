package com.app.miklink.core.domain.model

data class TestProfile(
    val profileId: Long,
    val profileName: String,
    val profileDescription: String?,
    val runTdr: Boolean,
    val runLinkStatus: Boolean,
    val runLldp: Boolean,
    val runPing: Boolean,
    val pingTarget1: String?,
    val pingTarget2: String?,
    val pingTarget3: String?,
    val pingCount: Int,
    val runSpeedTest: Boolean
)

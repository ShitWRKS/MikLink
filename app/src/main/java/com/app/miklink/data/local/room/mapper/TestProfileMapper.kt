/*
 * Purpose: Map Room TestProfileEntity to domain TestProfile and back, including thresholds serialization.
 * Inputs: Entities from Room and domain TestProfile instances with optional thresholds.
 * Outputs: Converted domain objects or entities ready for persistence.
 * Notes: Thresholds fall back to defaults if deserialization fails.
 */
package com.app.miklink.data.local.room.mapper

import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.data.local.room.entity.TestProfileEntity

fun TestProfileEntity.toDomain(): TestProfile {
    return TestProfile(
        profileId = profileId,
        profileName = profileName,
        profileDescription = profileDescription,
        runTdr = runTdr,
        runLinkStatus = runLinkStatus,
        runLldp = runLldp,
        runPing = runPing,
        pingTarget1 = pingTarget1,
        pingTarget2 = pingTarget2,
        pingTarget3 = pingTarget3,
        pingCount = pingCount,
        runSpeedTest = runSpeedTest,
        thresholds = thresholdsJson.toThresholdsOrDefault()
    )
}

fun TestProfile.toEntity(): TestProfileEntity {
    return TestProfileEntity(
        profileId = profileId,
        profileName = profileName,
        profileDescription = profileDescription,
        runTdr = runTdr,
        runLinkStatus = runLinkStatus,
        runLldp = runLldp,
        runPing = runPing,
        pingTarget1 = pingTarget1,
        pingTarget2 = pingTarget2,
        pingTarget3 = pingTarget3,
        pingCount = pingCount,
        runSpeedTest = runSpeedTest,
        thresholdsJson = thresholds.toJsonOrNull()
    )
}

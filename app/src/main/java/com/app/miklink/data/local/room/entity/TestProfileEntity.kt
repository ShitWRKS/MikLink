/*
 * Purpose: Room entity for test profiles including enabled steps, ping targets, and serialized thresholds.
 * Inputs: Profile fields from the UI/domain layer; thresholdsJson stores TestThresholds as JSON.
 * Outputs: Persisted rows backing domain TestProfile instances.
 * Notes: thresholdsJson keeps schema changes minimal while allowing future threshold extensions.
 */
package com.app.miklink.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_profiles")
data class TestProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val profileId: Long = 0,
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
    val runSpeedTest: Boolean,
    val thresholdsJson: String? = null
)

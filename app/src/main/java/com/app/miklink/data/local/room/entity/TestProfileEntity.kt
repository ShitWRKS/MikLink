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
    val runSpeedTest: Boolean
)

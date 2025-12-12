package com.app.miklink.core.data.local.room.v1.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_profiles")
data class TestProfile(
    @PrimaryKey(autoGenerate = true)
    val profileId: Long = 0,
    val profileName: String,
    val profileDescription: String?,
    val runTdr: Boolean,
    val runLinkStatus: Boolean,
    val runLldp: Boolean,
    val runPing: Boolean,
    // Ping targets
    val pingTarget1: String? = null,
    val pingTarget2: String? = null,
    val pingTarget3: String? = null,
    val pingCount: Int = 4, // numero ping per target (range 1-20)
    val runSpeedTest: Boolean = false
)
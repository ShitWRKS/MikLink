package com.app.miklink.data.db.model

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
    val runTraceroute: Boolean,
    // Temporary fields for Phase 3b
    val pingTarget1: String? = null,
    val pingTarget2: String? = null,
    val pingTarget3: String? = null
)
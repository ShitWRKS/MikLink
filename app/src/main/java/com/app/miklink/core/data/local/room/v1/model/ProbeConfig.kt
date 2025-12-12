package com.app.miklink.core.data.local.room.v1.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "probe_config")
data class ProbeConfig(
    @PrimaryKey(autoGenerate = true)
    val probeId: Long = 0,
    val ipAddress: String,
    val username: String,
    val password: String,
    val testInterface: String,
    val isOnline: Boolean,
    val modelName: String?,
    val tdrSupported: Boolean,
    val isHttps: Boolean = false
)
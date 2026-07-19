package com.app.miklink.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.miklink.core.domain.model.TdrCapability

@Entity(tableName = "probe_config")
data class ProbeConfigEntity(
    @PrimaryKey
    val id: Int = 1, // Singleton: PK fissa
    val ipAddress: String,
    val username: String,
    val password: String,
    val testInterface: String,
    val isHttps: Boolean,
    val isOnline: Boolean,
    val modelName: String?,
    val tdrCapability: String
) {
    val tdrSupported: Boolean
        get() = tdrCapability == TdrCapability.SUPPORTED.name
}

package com.app.miklink.data.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true)
    val clientId: Long = 0,
    val companyName: String,
    val location: String? = "Sede",
    val notes: String?,
    val networkMode: String,
    val vlanId: Int?,
    val staticIp: String?,
    val staticSubnet: String?,
    val staticGateway: String?,
    val pingTarget1: String?,
    val pingTarget2: String?,
    val pingTarget3: String?,
    val idPrefix: String = "A",
    val nextIdNumber: Int = 1,
    val lastFloor: String? = null,
    val lastRoom: String? = null
)
package com.app.miklink.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    indices = [Index(value = ["companyName"])]
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    val clientId: Long = 0,
    val companyName: String,
    val location: String?,
    val notes: String?,
    val networkMode: String,
    val staticIp: String?,
    val staticSubnet: String?,
    val staticGateway: String?,
    val staticCidr: String?,
    val minLinkRate: String,
    val socketPrefix: String,
    val socketSuffix: String,
    val socketSeparator: String,
    val socketNumberPadding: Int,
    val nextIdNumber: Int,
    val speedTestServerAddress: String?,
    val speedTestServerUser: String?,
    val speedTestServerPassword: String?
)

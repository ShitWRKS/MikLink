package com.app.miklink.data.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    indices = [
        androidx.room.Index(value = ["companyName"])
    ]
)
data class Client(
    @PrimaryKey(autoGenerate = true)
    val clientId: Long = 0,
    val companyName: String,
    val location: String? = "Sede",
    val notes: String?,
    val networkMode: String,
    val staticIp: String?,
    val staticSubnet: String?,
    val staticGateway: String?,
    // Nuovo: preferire CIDR rispetto a IP+Subnet legacy
    val staticCidr: String? = null,
    // Nuovo: soglia minima link per PASS ("10M","100M","1G","10G")
    val minLinkRate: String = "1G",
    val socketPrefix: String = "",
    // Nuovi campi per formattazione ID presa
    val socketSuffix: String = "",
    val socketSeparator: String = "-",
    val socketNumberPadding: Int = 1,
    val nextIdNumber: Int = 1,
    val lastFloor: String? = null,
    val lastRoom: String? = null,
    // Speed Test configuration
    val speedTestServerAddress: String? = null,
    val speedTestServerUser: String? = null,
    val speedTestServerPassword: String? = null
)
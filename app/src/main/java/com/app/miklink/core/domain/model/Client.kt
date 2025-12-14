package com.app.miklink.core.domain.model

data class Client(
    val clientId: Long,
    val companyName: String,
    val location: String?,
    val notes: String?,
    val networkMode: NetworkMode,
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

enum class NetworkMode {
    DHCP, STATIC
}

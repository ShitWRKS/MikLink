/*
 * Purpose: Domain model for client configuration including socket-id formatting fields used across the app.
 * Inputs: Client properties such as network configuration, socket prefix/separator/padding/suffix, and speed test settings.
 * Outputs: Immutable client instances and deterministic socket-id formatting via socketNameFor().
 * Notes: Remains pure Kotlin; socketNameFor delegates to SocketIdLite to keep ADR-0004 formatting consistent across layers.
 */
package com.app.miklink.core.domain.model

import com.app.miklink.core.domain.policy.socketid.SocketIdLite

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

/**
 * Deterministic socket name builder using the client's socket formatting fields.
 * This function is pure and does not mutate state.
 */
fun Client.socketNameFor(idNumber: Int): String {
    return SocketIdLite.format(
        prefix = socketPrefix,
        separator = socketSeparator,
        numberPadding = socketNumberPadding,
        suffix = socketSuffix,
        idNumber = idNumber
    )
}

package com.app.miklink.data.local.room.mapper

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.data.local.room.entity.ClientEntity

fun ClientEntity.toDomain(): Client {
    return Client(
        clientId = clientId,
        companyName = companyName,
        location = location,
        notes = notes,
        networkMode = when (networkMode) {
            "STATIC" -> NetworkMode.STATIC
            else -> NetworkMode.DHCP
        },
        staticIp = staticIp,
        staticSubnet = staticSubnet,
        staticGateway = staticGateway,
        staticCidr = staticCidr,
        minLinkRate = minLinkRate,
        socketPrefix = socketPrefix,
        socketSuffix = socketSuffix,
        socketSeparator = socketSeparator,
        socketNumberPadding = socketNumberPadding,
        nextIdNumber = nextIdNumber,
        speedTestServerAddress = speedTestServerAddress,
        speedTestServerUser = speedTestServerUser,
        speedTestServerPassword = speedTestServerPassword
    )
}

fun Client.toEntity(): ClientEntity {
    return ClientEntity(
        clientId = clientId,
        companyName = companyName,
        location = location,
        notes = notes,
        networkMode = networkMode.name,
        staticIp = staticIp,
        staticSubnet = staticSubnet,
        staticGateway = staticGateway,
        staticCidr = staticCidr,
        minLinkRate = minLinkRate,
        socketPrefix = socketPrefix,
        socketSuffix = socketSuffix,
        socketSeparator = socketSeparator,
        socketNumberPadding = socketNumberPadding,
        nextIdNumber = nextIdNumber,
        speedTestServerAddress = speedTestServerAddress,
        speedTestServerUser = speedTestServerUser,
        speedTestServerPassword = speedTestServerPassword
    )
}

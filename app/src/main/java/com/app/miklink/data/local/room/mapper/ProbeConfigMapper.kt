package com.app.miklink.data.local.room.mapper

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.local.room.entity.ProbeConfigEntity

fun ProbeConfigEntity.toDomain(): ProbeConfig {
    return ProbeConfig(
        ipAddress = ipAddress,
        username = username,
        password = password,
        testInterface = testInterface,
        isHttps = isHttps,
        isOnline = isOnline,
        modelName = modelName,
        tdrSupported = tdrSupported
    )
}

fun ProbeConfig.toEntity(): ProbeConfigEntity {
    return ProbeConfigEntity(
        id = 1, // Singleton
        ipAddress = ipAddress,
        username = username,
        password = password,
        testInterface = testInterface,
        isHttps = isHttps,
        isOnline = isOnline,
        modelName = modelName,
        tdrSupported = tdrSupported
    )
}

package com.app.miklink.core.data.repository.probe

import com.app.miklink.core.data.local.room.v1.model.ProbeConfig

/**
 * Repository per accesso ai dati ProbeConfig.
 */
interface ProbeRepository {
    suspend fun getProbe(id: Long): ProbeConfig?
}


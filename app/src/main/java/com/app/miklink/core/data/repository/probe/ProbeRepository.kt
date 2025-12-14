package com.app.miklink.core.data.repository.probe

import com.app.miklink.core.domain.model.ProbeConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository per accesso ai dati ProbeConfig (singleton).
 */
interface ProbeRepository {
    fun observeProbeConfig(): Flow<ProbeConfig?>
    suspend fun getProbeConfig(): ProbeConfig?
    suspend fun saveProbeConfig(config: ProbeConfig)
}


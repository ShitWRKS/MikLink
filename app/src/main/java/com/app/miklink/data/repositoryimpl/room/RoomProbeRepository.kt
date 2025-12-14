package com.app.miklink.data.repositoryimpl.room

import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.local.room.dao.ProbeConfigDao
import com.app.miklink.data.local.room.mapper.toDomain
import com.app.miklink.data.local.room.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomProbeRepository @Inject constructor(
    private val probeConfigDao: ProbeConfigDao
) : ProbeRepository {
    override fun observeProbeConfig(): Flow<ProbeConfig?> {
        return probeConfigDao.observe().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getProbeConfig(): ProbeConfig? {
        return probeConfigDao.get()?.toDomain()
    }

    override suspend fun saveProbeConfig(config: ProbeConfig) {
        probeConfigDao.upsert(config.toEntity())
    }
}

package com.app.miklink.data.repositoryimpl.roomv1

import com.app.miklink.core.data.local.room.v1.dao.ProbeConfigDao
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.repository.probe.ProbeRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Implementazione Room-backed di ProbeRepository.
 */
class RoomV1ProbeRepository @Inject constructor(
    private val probeConfigDao: ProbeConfigDao
) : ProbeRepository {
    override suspend fun getProbe(id: Long): ProbeConfig? {
        return probeConfigDao.getProbeById(id).firstOrNull()
    }
}


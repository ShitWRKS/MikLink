package com.app.miklink.data.repositoryimpl.roomv1

import com.app.miklink.core.data.local.room.v1.dao.TestProfileDao
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import com.app.miklink.core.data.repository.test.TestProfileRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Implementazione Room-backed di TestProfileRepository.
 */
class RoomV1TestProfileRepository @Inject constructor(
    private val testProfileDao: TestProfileDao
) : TestProfileRepository {
    override suspend fun getProfile(id: Long): TestProfile? {
        return testProfileDao.getProfileById(id).firstOrNull()
    }
}


package com.app.miklink.data.repositoryimpl.room

import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.data.local.room.dao.TestProfileDao
import com.app.miklink.data.local.room.mapper.toDomain
import com.app.miklink.data.local.room.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTestProfileRepository @Inject constructor(
    private val testProfileDao: TestProfileDao
) : TestProfileRepository {
    override fun observeAllProfiles(): Flow<List<TestProfile>> {
        return testProfileDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProfile(id: Long): TestProfile? {
        return testProfileDao.getById(id)?.toDomain()
    }

    override suspend fun insertProfile(profile: TestProfile): Long {
        return testProfileDao.insert(profile.toEntity())
    }

    override suspend fun updateProfile(profile: TestProfile) {
        testProfileDao.update(profile.toEntity())
    }

    override suspend fun deleteProfile(profile: TestProfile) {
        testProfileDao.delete(profile.toEntity())
    }
}

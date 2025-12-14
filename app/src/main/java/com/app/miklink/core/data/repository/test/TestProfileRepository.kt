package com.app.miklink.core.data.repository.test

import com.app.miklink.core.domain.model.TestProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository per accesso ai dati TestProfile.
 */
interface TestProfileRepository {
    fun observeAllProfiles(): Flow<List<TestProfile>>
    suspend fun getProfile(id: Long): TestProfile?
    suspend fun insertProfile(profile: TestProfile): Long
    suspend fun updateProfile(profile: TestProfile)
    suspend fun deleteProfile(profile: TestProfile)
}


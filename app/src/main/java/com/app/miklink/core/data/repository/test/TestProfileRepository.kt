package com.app.miklink.core.data.repository.test

import com.app.miklink.core.data.local.room.v1.model.TestProfile

/**
 * Repository per accesso ai dati TestProfile.
 */
interface TestProfileRepository {
    suspend fun getProfile(id: Long): TestProfile?
}


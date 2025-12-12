package com.app.miklink.data.repositoryimpl.roomv1

import com.app.miklink.core.data.local.room.v1.dao.ClientDao
import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.repository.client.ClientRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Implementazione Room-backed di ClientRepository.
 */
class RoomV1ClientRepository @Inject constructor(
    private val clientDao: ClientDao
) : ClientRepository {
    override suspend fun getClient(id: Long): Client? {
        return clientDao.getClientById(id).firstOrNull()
    }
}


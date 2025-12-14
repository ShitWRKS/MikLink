package com.app.miklink.data.repositoryimpl.room

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.data.local.room.dao.ClientDao
import com.app.miklink.data.local.room.mapper.toDomain
import com.app.miklink.data.local.room.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomClientRepository @Inject constructor(
    private val clientDao: ClientDao
) : ClientRepository {
    override fun observeAllClients(): Flow<List<Client>> {
        return clientDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getClient(id: Long): Client? {
        return clientDao.getById(id)?.toDomain()
    }

    override suspend fun insertClient(client: Client): Long {
        return clientDao.insert(client.toEntity())
    }

    override suspend fun updateClient(client: Client) {
        clientDao.update(client.toEntity())
    }

    override suspend fun deleteClient(client: Client) {
        clientDao.delete(client.toEntity())
    }
}

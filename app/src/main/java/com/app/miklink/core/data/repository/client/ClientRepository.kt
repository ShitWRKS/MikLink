package com.app.miklink.core.data.repository.client

import com.app.miklink.core.domain.model.Client
import kotlinx.coroutines.flow.Flow

/**
 * Repository per accesso ai dati Client.
 */
interface ClientRepository {
    fun observeAllClients(): Flow<List<Client>>
    suspend fun getClient(id: Long): Client?
    suspend fun insertClient(client: Client): Long
    suspend fun updateClient(client: Client)
    suspend fun deleteClient(client: Client)
}


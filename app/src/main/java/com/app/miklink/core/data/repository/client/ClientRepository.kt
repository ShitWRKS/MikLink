package com.app.miklink.core.data.repository.client

import com.app.miklink.core.data.local.room.v1.model.Client

/**
 * Repository per accesso ai dati Client.
 */
interface ClientRepository {
    suspend fun getClient(id: Long): Client?
}


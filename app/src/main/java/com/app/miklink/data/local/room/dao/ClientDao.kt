package com.app.miklink.data.local.room.dao

import androidx.room.*
import com.app.miklink.data.local.room.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients")
    fun observeAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE clientId = :id")
    suspend fun getById(id: Long): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity)

    @Delete
    suspend fun delete(client: ClientEntity)
}

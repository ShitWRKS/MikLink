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

    /**
     * Atomic counter increment (ADR-0010/ADR-0013). Returns the number of rows updated:
     * 0 means the client does not exist (caller must roll back).
     */
    @Query("UPDATE clients SET nextIdNumber = nextIdNumber + 1 WHERE clientId = :clientId")
    suspend fun incrementNextIdNumber(clientId: Long): Int
}

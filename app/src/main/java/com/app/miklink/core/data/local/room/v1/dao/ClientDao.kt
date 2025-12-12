package com.app.miklink.core.data.local.room.v1.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.miklink.core.data.local.room.v1.model.Client
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT * FROM clients ORDER BY companyName ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE clientId = :id")
    fun getClientById(id: Long): Flow<Client?>

    @Query("UPDATE clients SET nextIdNumber = nextIdNumber + 1 WHERE clientId = :id")
    suspend fun incrementNextIdNumber(id: Long)
}
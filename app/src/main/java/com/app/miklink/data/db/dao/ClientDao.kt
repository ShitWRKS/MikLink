package com.app.miklink.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.miklink.data.db.model.Client
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

    @Query("UPDATE clients SET nextIdNumber = nextIdNumber + 1, lastFloor = :floor, lastRoom = :room WHERE clientId = :id")
    suspend fun updateNextIdAndStickyFields(id: Long, floor: String?, room: String?)
}
package com.app.miklink.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.miklink.data.db.model.ProbeConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ProbeConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(probe: ProbeConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(probes: List<ProbeConfig>)

    @Update
    suspend fun update(probe: ProbeConfig)

    @Delete
    suspend fun delete(probe: ProbeConfig)

    @Query("DELETE FROM probe_config")
    suspend fun deleteAll()

    // name column removed; order by probeId to keep deterministic ordering
    @Query("SELECT * FROM probe_config ORDER BY probeId ASC")
    fun getAllProbes(): Flow<List<ProbeConfig>>

    @Query("SELECT * FROM probe_config WHERE probeId = :id")
    fun getProbeById(id: Long): Flow<ProbeConfig?>

    // NUOVO: Sonda unica (post-refactor)
    @Query("SELECT * FROM probe_config ORDER BY probeId ASC LIMIT 1")
    fun getSingleProbe(): Flow<ProbeConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSingle(probe: ProbeConfig)
}
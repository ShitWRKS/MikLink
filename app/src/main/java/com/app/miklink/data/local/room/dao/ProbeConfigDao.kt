package com.app.miklink.data.local.room.dao

import androidx.room.*
import com.app.miklink.data.local.room.entity.ProbeConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProbeConfigDao {
    @Query("SELECT * FROM probe_config WHERE id = 1")
    fun observe(): Flow<ProbeConfigEntity?>

    @Query("SELECT * FROM probe_config WHERE id = 1")
    suspend fun get(): ProbeConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: ProbeConfigEntity)
}

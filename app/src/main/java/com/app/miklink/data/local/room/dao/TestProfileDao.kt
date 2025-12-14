package com.app.miklink.data.local.room.dao

import androidx.room.*
import com.app.miklink.data.local.room.entity.TestProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestProfileDao {
    @Query("SELECT * FROM test_profiles ORDER BY profileName")
    fun observeAll(): Flow<List<TestProfileEntity>>

    @Query("SELECT * FROM test_profiles WHERE profileId = :id")
    suspend fun getById(id: Long): TestProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: TestProfileEntity): Long

    @Update
    suspend fun update(profile: TestProfileEntity)

    @Delete
    suspend fun delete(profile: TestProfileEntity)
}

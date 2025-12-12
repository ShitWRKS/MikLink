package com.app.miklink.core.data.local.room.v1.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface TestProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: TestProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<TestProfile>)

    @Update
    suspend fun update(profile: TestProfile)

    @Delete
    suspend fun delete(profile: TestProfile)

    @Query("DELETE FROM test_profiles")
    suspend fun deleteAll()

    @Query("SELECT * FROM test_profiles ORDER BY profileName ASC")
    fun getAllProfiles(): Flow<List<TestProfile>>

    @Query("SELECT * FROM test_profiles WHERE profileId = :id")
    fun getProfileById(id: Long): Flow<TestProfile?>
    
    @Query("SELECT * FROM test_profiles WHERE profileName = :name LIMIT 1")
    fun getProfileByName(name: String): Flow<TestProfile?>
}
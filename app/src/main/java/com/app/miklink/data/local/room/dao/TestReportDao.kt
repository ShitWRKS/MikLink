package com.app.miklink.data.local.room.dao

import androidx.room.*
import com.app.miklink.data.local.room.entity.TestReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestReportDao {
    @Query("SELECT * FROM test_reports ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TestReportEntity>>

    @Query("SELECT * FROM test_reports WHERE clientId = :clientId ORDER BY timestamp DESC")
    fun observeByClient(clientId: Long): Flow<List<TestReportEntity>>

    @Query("SELECT * FROM test_reports WHERE reportId = :id")
    suspend fun getById(id: Long): TestReportEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(report: TestReportEntity): Long

    @Delete
    suspend fun delete(report: TestReportEntity)
}

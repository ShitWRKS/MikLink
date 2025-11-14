package com.app.miklink.data.db.dao

import androidx.room.*
import com.app.miklink.data.db.model.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: Report)

    @Update
    suspend fun update(report: Report)

    @Delete
    suspend fun delete(report: Report)

    @Query("SELECT * FROM test_reports WHERE reportId = :id")
    fun getReportById(id: Long): Flow<Report?>

    @Query("SELECT * FROM test_reports WHERE reportId = :id LIMIT 1")
    suspend fun getReportByIdOnce(id: Long): Report?

    @Query("SELECT * FROM test_reports WHERE clientId = :clientId ORDER BY timestamp DESC")
    fun getReportsForClient(clientId: Long): Flow<List<Report>>

    @Query("SELECT * FROM test_reports WHERE clientId = :clientId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastReportForClient(clientId: Long): Report?

    @Query("SELECT * FROM test_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<Report>>
}

package com.app.miklink.core.data.repository.report

import com.app.miklink.core.domain.model.TestReport
import kotlinx.coroutines.flow.Flow

/**
 * Repository per accesso ai dati TestReport.
 */
interface ReportRepository {
    fun observeAllReports(): Flow<List<TestReport>>
    fun observeReportsByClient(clientId: Long): Flow<List<TestReport>>
    suspend fun getReport(id: Long): TestReport?
    suspend fun saveReport(report: TestReport): Long
    suspend fun deleteReport(report: TestReport)
}


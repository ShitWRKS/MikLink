package com.app.miklink.data.repositoryimpl.room

import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.data.local.room.dao.TestReportDao
import com.app.miklink.data.local.room.mapper.toDomain
import com.app.miklink.data.local.room.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomReportRepository @Inject constructor(
    private val testReportDao: TestReportDao
) : ReportRepository {
    override fun observeAllReports(): Flow<List<TestReport>> {
        return testReportDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeReportsByClient(clientId: Long): Flow<List<TestReport>> {
        return testReportDao.observeByClient(clientId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getReport(id: Long): TestReport? {
        return testReportDao.getById(id)?.toDomain()
    }

    override suspend fun saveReport(report: TestReport): Long {
        return testReportDao.insert(report.toEntity())
    }

    override suspend fun deleteReport(report: TestReport) {
        testReportDao.delete(report.toEntity())
    }
}

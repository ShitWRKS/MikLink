package com.app.miklink.data.repositoryimpl.roomv1

import com.app.miklink.core.data.local.room.v1.dao.ReportDao
import com.app.miklink.core.data.local.room.v1.model.Report
import com.app.miklink.core.data.repository.report.ReportRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Implementazione Room-backed di ReportRepository.
 */
class RoomV1ReportRepository @Inject constructor(
    private val reportDao: ReportDao
) : ReportRepository {
    override suspend fun saveReport(report: Report): Long {
        reportDao.insert(report)
        return report.reportId
    }

    override suspend fun getReport(id: Long): Report? {
        return reportDao.getReportByIdOnce(id)
    }
}


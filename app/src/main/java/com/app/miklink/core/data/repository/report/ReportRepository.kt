package com.app.miklink.core.data.repository.report

import com.app.miklink.core.data.local.room.v1.model.Report

/**
 * Repository per accesso ai dati Report.
 */
interface ReportRepository {
    suspend fun saveReport(report: Report): Long
    suspend fun getReport(id: Long): Report?
}


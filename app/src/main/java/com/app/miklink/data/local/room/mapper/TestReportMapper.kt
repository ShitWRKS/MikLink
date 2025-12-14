package com.app.miklink.data.local.room.mapper

import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.data.local.room.entity.TestReportEntity

fun TestReportEntity.toDomain(): TestReport {
    return TestReport(
        reportId = reportId,
        clientId = clientId,
        timestamp = timestamp,
        socketName = socketName,
        notes = notes,
        probeName = probeName,
        profileName = profileName,
        overallStatus = overallStatus,
        resultFormatVersion = resultFormatVersion,
        resultsJson = resultsJson
    )
}

fun TestReport.toEntity(): TestReportEntity {
    return TestReportEntity(
        reportId = reportId,
        clientId = clientId,
        timestamp = timestamp,
        socketName = socketName,
        notes = notes,
        probeName = probeName,
        profileName = profileName,
        overallStatus = overallStatus,
        resultFormatVersion = resultFormatVersion,
        resultsJson = resultsJson
    )
}

package com.app.miklink.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_reports",
    indices = [
        Index(value = ["clientId"]),
        Index(value = ["timestamp"]),
        Index(value = ["clientId", "timestamp"])
    ]
)
data class TestReportEntity(
    @PrimaryKey(autoGenerate = true)
    val reportId: Long = 0,
    val clientId: Long?,
    val timestamp: Long,
    val socketName: String?,
    val notes: String?,
    val probeName: String?,
    val profileName: String?,
    val overallStatus: String,
    val resultFormatVersion: Int,
    val resultsJson: String
)

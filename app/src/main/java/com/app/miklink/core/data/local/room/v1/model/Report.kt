package com.app.miklink.core.data.local.room.v1.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_reports",
    indices = [
        androidx.room.Index(value = ["clientId"]),
        androidx.room.Index(value = ["timestamp"]),
        androidx.room.Index(value = ["clientId", "timestamp"]) // Composite index for common query
    ]
)
data class Report(
    @PrimaryKey(autoGenerate = true)
    val reportId: Long = 0,
    val clientId: Long?,
    val timestamp: Long,
    val socketName: String?,
    val notes: String?,
    val probeName: String?,
    val profileName: String?,
    val overallStatus: String,
    val resultsJson: String
)
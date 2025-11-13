package com.app.miklink.data.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_reports")
data class Report(
    @PrimaryKey(autoGenerate = true)
    val reportId: Long = 0,
    val clientId: Long?,
    val timestamp: Long,
    val socketName: String?,
    val floor: String?,
    val room: String?,
    val notes: String?,
    val probeName: String?,
    val profileName: String?,
    val overallStatus: String,
    val resultsJson: String
)
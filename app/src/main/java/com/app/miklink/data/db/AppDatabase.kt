package com.app.miklink.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.db.model.TestProfile

@Database(
    entities = [Client::class, ProbeConfig::class, TestProfile::class, Report::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun probeConfigDao(): ProbeConfigDao
    abstract fun reportDao(): ReportDao
    abstract fun testProfileDao(): TestProfileDao
}
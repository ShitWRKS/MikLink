package com.app.miklink.core.data.local.room.v1

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.miklink.core.data.local.room.v1.dao.ClientDao
import com.app.miklink.core.data.local.room.v1.dao.ProbeConfigDao
import com.app.miklink.core.data.local.room.v1.dao.ReportDao
import com.app.miklink.core.data.local.room.v1.dao.TestProfileDao
import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.local.room.v1.model.Report
import com.app.miklink.core.data.local.room.v1.model.TestProfile

/**
 * Restored single AppDatabase annotated for Room.
 * Uses existing entities and DAOs in `com.app.miklink.core.data.local.room.v1`.
 * Version is derived from existing migrations (last migration target is 13).
 */
@Database(
    entities = [
        Client::class,
        ProbeConfig::class,
        TestProfile::class,
        Report::class
    ],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun probeConfigDao(): ProbeConfigDao
    abstract fun reportDao(): ReportDao
    abstract fun testProfileDao(): TestProfileDao
}
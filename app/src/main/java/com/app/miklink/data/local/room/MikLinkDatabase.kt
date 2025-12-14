package com.app.miklink.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.miklink.data.local.room.dao.ClientDao
import com.app.miklink.data.local.room.dao.ProbeConfigDao
import com.app.miklink.data.local.room.dao.TestProfileDao
import com.app.miklink.data.local.room.dao.TestReportDao
import com.app.miklink.data.local.room.entity.ClientEntity
import com.app.miklink.data.local.room.entity.ProbeConfigEntity
import com.app.miklink.data.local.room.entity.TestProfileEntity
import com.app.miklink.data.local.room.entity.TestReportEntity

@Database(
    entities = [
        ClientEntity::class,
        ProbeConfigEntity::class,
        TestProfileEntity::class,
        TestReportEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MikLinkDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun probeConfigDao(): ProbeConfigDao
    abstract fun testProfileDao(): TestProfileDao
    abstract fun testReportDao(): TestReportDao
}

package com.app.miklink.data.local.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE test_profiles ADD COLUMN thresholdsJson TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS probe_config_new (" +
                "id INTEGER NOT NULL, ipAddress TEXT NOT NULL, username TEXT NOT NULL, " +
                "password TEXT NOT NULL, testInterface TEXT NOT NULL, isHttps INTEGER NOT NULL, " +
                "isOnline INTEGER NOT NULL, modelName TEXT, tdrCapability TEXT NOT NULL, PRIMARY KEY(id))"
        )
        database.execSQL(
            "INSERT INTO probe_config_new (id, ipAddress, username, password, testInterface, isHttps, isOnline, modelName, tdrCapability) " +
                "SELECT id, ipAddress, username, password, testInterface, isHttps, isOnline, modelName, " +
                "CASE WHEN tdrSupported = 1 THEN 'SUPPORTED' ELSE 'UNSUPPORTED' END FROM probe_config"
        )
        database.execSQL("DROP TABLE probe_config")
        database.execSQL("ALTER TABLE probe_config_new RENAME TO probe_config")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

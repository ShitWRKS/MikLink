package com.app.miklink.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Oggetto contenente tutte le migrazioni del database.
 * Accessibile sia dal DatabaseModule che dai test di migrazione.
 */
object Migrations {

    // Migrazione v7 → v8: aggiunta colonna pingCount a test_profiles
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE test_profiles ADD COLUMN pingCount INTEGER NOT NULL DEFAULT 4")
        }
    }

    // Migrazione v8 → v9: aggiunta campi Speed Test a clients
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerAddress TEXT")
            database.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerUser TEXT")
            database.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerPassword TEXT")
        }
    }

    // Migrazione v9 → v10: aggiunta flag runSpeedTest a test_profiles
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE test_profiles ADD COLUMN runSpeedTest INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Array di tutte le migrazioni in ordine.
     * Utile per aggiungere tutte le migrazioni al database.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10
    )
}


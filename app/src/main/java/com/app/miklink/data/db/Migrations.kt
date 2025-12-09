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
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE test_profiles ADD COLUMN pingCount INTEGER NOT NULL DEFAULT 4")
        }
    }

    // Migrazione v8 → v9: aggiunta campi Speed Test a clients
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerAddress TEXT")
            db.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerUser TEXT")
            db.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerPassword TEXT")
        }
    }

    // Migrazione v9 → v10: aggiunta flag runSpeedTest a test_profiles
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE test_profiles ADD COLUMN runSpeedTest INTEGER NOT NULL DEFAULT 0")
        }
    }

    // Migrazione v10 -> v11: rimozione colonna 'name' dalla tabella probe_config
    // Room non supporta DROP COLUMN, quindi creiamo una nuova tabella senza la colonna,
    // copiamo i dati, cancelliamo la vecchia tabella e rinominiamo la nuova
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS probe_config_new (probeId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ipAddress TEXT NOT NULL, username TEXT NOT NULL, password TEXT NOT NULL, testInterface TEXT NOT NULL, isOnline INTEGER NOT NULL, modelName TEXT, tdrSupported INTEGER NOT NULL, isHttps INTEGER NOT NULL DEFAULT 0)")
            // Copy: because previous table had 'name' column, map remaining columns
            db.execSQL("INSERT INTO probe_config_new (probeId, ipAddress, username, password, testInterface, isOnline, modelName, tdrSupported, isHttps) SELECT probeId, ipAddress, username, password, testInterface, isOnline, modelName, tdrSupported, isHttps FROM probe_config")
            db.execSQL("DROP TABLE probe_config")
            db.execSQL("ALTER TABLE probe_config_new RENAME TO probe_config")
        }
    }

    // Migrazione v11 -> v12: aggiunta campi socketSuffix, socketSeparator, socketNumberPadding a clients
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Aggiungiamo colonne con valori di default per mantenere la compatibilità
            db.execSQL("ALTER TABLE clients ADD COLUMN socketSuffix TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE clients ADD COLUMN socketSeparator TEXT NOT NULL DEFAULT '-'")
            db.execSQL("ALTER TABLE clients ADD COLUMN socketNumberPadding INTEGER NOT NULL DEFAULT 1")
        }
    }

    // Migrazione v12 -> v13: aggiunta indici per performance
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Indices for Client entity
            db.execSQL("CREATE INDEX IF NOT EXISTS index_clients_companyName ON clients(companyName)")
            
            // Indices for Report entity
            db.execSQL("CREATE INDEX IF NOT EXISTS index_test_reports_clientId ON test_reports(clientId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_test_reports_timestamp ON test_reports(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_test_reports_clientId_timestamp ON test_reports(clientId, timestamp)")
        }
    }

    /**
     * Array di tutte le migrazioni in ordine.
     * Utile per aggiungere tutte le migrazioni al database.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13
    )
}


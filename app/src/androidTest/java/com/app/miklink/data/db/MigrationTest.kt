package com.app.miklink.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumentation Test per validare tutte le migrazioni del database Room.
 *
 * Questo test è critico per garantire che gli aggiornamenti dell'app
 * non causino perdita di dati o corruzione del database degli utenti.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test che esegue tutte le migrazioni in sequenza dalla v7 alla v10.
     *
     * Questo approccio garantisce che:
     * 1. Ogni migrazione sia sintatticamente corretta
     * 2. Le migrazioni possano essere applicate in sequenza
     * 3. Lo schema finale sia valido
     */
    @Test
    @Throws(IOException::class)
    fun runAllMigrations_from7to10_succeeds() {
        // 1. Crea il database alla versione 7 (prima versione con migrazioni definite)
        var db = helper.createDatabase(TEST_DB, 7)

        // Inserisci dati di test nella v7 per validare la preservazione dei dati
        db.execSQL(
            """INSERT INTO test_profiles (
                profileId, profileName, profileDescription, 
                runTdr, runLinkStatus, runLldp, runPing
            ) VALUES (1, 'Test Profile', 'Test Description', 1, 1, 1, 1)"""
        )

        db.execSQL(
            """INSERT INTO clients (
                clientId, companyName, networkMode, 
                staticIp, staticSubnet, staticGateway, staticCidr,
                minLinkRate, socketPrefix, nextIdNumber
            ) VALUES (1, 'Test Company', 'DHCP', '', '', '', '', '100M', 'P', 1)"""
        )

        db.close()

        // 2. Esegui MIGRATION_7_8 (aggiunge pingCount a test_profiles)
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            8,
            true,
            Migrations.MIGRATION_7_8
        )

        // Verifica che i dati esistenti siano preservati e che la nuova colonna esista
        val cursor78 = db.query("SELECT profileId, profileName, pingCount FROM test_profiles WHERE profileId = 1")
        cursor78.use {
            assert(it.moveToFirst())
            assertEquals(1, it.getLong(it.getColumnIndexOrThrow("profileId")))
            assertEquals("Test Profile", it.getString(it.getColumnIndexOrThrow("profileName")))
            // pingCount dovrebbe avere il valore di default 4
            assertEquals(4, it.getInt(it.getColumnIndexOrThrow("pingCount")))
        }

        db.close()

        // 3. Esegui MIGRATION_8_9 (aggiunge campi Speed Test a clients)
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            9,
            true,
            Migrations.MIGRATION_8_9
        )

        // Verifica che i dati del client siano preservati e che le nuove colonne esistano
        val cursor89 = db.query(
            """SELECT clientId, companyName, speedTestServerAddress, 
               speedTestServerUser, speedTestServerPassword 
               FROM clients WHERE clientId = 1"""
        )
        cursor89.use {
            assert(it.moveToFirst())
            assertEquals(1, it.getLong(it.getColumnIndexOrThrow("clientId")))
            assertEquals("Test Company", it.getString(it.getColumnIndexOrThrow("companyName")))
            // Le nuove colonne dovrebbero essere NULL per i dati esistenti
            val addressIndex = it.getColumnIndexOrThrow("speedTestServerAddress")
            val userIndex = it.getColumnIndexOrThrow("speedTestServerUser")
            val passwordIndex = it.getColumnIndexOrThrow("speedTestServerPassword")
            assert(it.isNull(addressIndex))
            assert(it.isNull(userIndex))
            assert(it.isNull(passwordIndex))
        }

        db.close()

        // 4. Esegui MIGRATION_9_10 (aggiunge runSpeedTest a test_profiles)
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            10,
            true,
            Migrations.MIGRATION_9_10
        )

        // Verifica che il profilo di test abbia la nuova colonna runSpeedTest
        val cursor910 = db.query(
            "SELECT profileId, profileName, pingCount, runSpeedTest FROM test_profiles WHERE profileId = 1"
        )
        cursor910.use {
            assert(it.moveToFirst())
            assertEquals(1, it.getLong(it.getColumnIndexOrThrow("profileId")))
            assertEquals("Test Profile", it.getString(it.getColumnIndexOrThrow("profileName")))
            assertEquals(4, it.getInt(it.getColumnIndexOrThrow("pingCount")))
            // runSpeedTest dovrebbe avere il valore di default 0 (false)
            assertEquals(0, it.getInt(it.getColumnIndexOrThrow("runSpeedTest")))
        }

        db.close()
    }

    /**
     * Test che verifica la migrazione "diretta" dalla v7 alla v10
     * applicando tutte le migrazioni in un'unica operazione.
     *
     * Questo simula il caso di un utente che aggiorna da una versione
     * molto vecchia direttamente all'ultima.
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll_from7to10_succeeds() {
        // Crea DB alla v7
        var db = helper.createDatabase(TEST_DB, 7)

        // Inserisci dati di test
        db.execSQL(
            """INSERT INTO test_profiles (
                profileId, profileName, profileDescription, 
                runTdr, runLinkStatus, runLldp, runPing
            ) VALUES (1, 'Migration Test', 'Test All Migrations', 1, 0, 1, 0)"""
        )

        db.close()

        // Applica TUTTE le migrazioni in un'unica operazione
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            10,
            true,
            Migrations.MIGRATION_7_8,
            Migrations.MIGRATION_8_9,
            Migrations.MIGRATION_9_10
        )

        // Verifica che tutte le colonne esistano
        val cursor = db.query(
            "SELECT profileId, profileName, pingCount, runSpeedTest FROM test_profiles WHERE profileId = 1"
        )
        cursor.use {
            assert(it.moveToFirst())
            assertEquals(1, it.getLong(it.getColumnIndexOrThrow("profileId")))
            assertEquals("Migration Test", it.getString(it.getColumnIndexOrThrow("profileName")))
            assertEquals(4, it.getInt(it.getColumnIndexOrThrow("pingCount")))
            assertEquals(0, it.getInt(it.getColumnIndexOrThrow("runSpeedTest")))
        }

        db.close()
    }

    /**
     * Test che verifica che i dati inseriti dopo ogni migrazione
     * siano correttamente persistiti.
     */
    @Test
    @Throws(IOException::class)
    fun migrations_preserveDataIntegrity() {
        // Crea DB v7 con dati più complessi
        var db = helper.createDatabase(TEST_DB, 7)

        // Inserisci più record per verificare l'integrità
        for (i in 1..5) {
            db.execSQL(
                """INSERT INTO test_profiles (
                    profileId, profileName, profileDescription, 
                    runTdr, runLinkStatus, runLldp, runPing
                ) VALUES ($i, 'Profile $i', 'Description $i', 1, 1, 1, 1)"""
            )
        }

        db.close()

        // Migra v7 → v8
        db = helper.runMigrationsAndValidate(TEST_DB, 8, true, Migrations.MIGRATION_7_8)

        // Verifica che tutti i 5 record esistano ancora
        val cursor8 = db.query("SELECT COUNT(*) FROM test_profiles")
        cursor8.use {
            assert(it.moveToFirst())
            assertEquals(5, it.getInt(0))
        }
        db.close()

        // Migra v8 → v9
        db = helper.runMigrationsAndValidate(TEST_DB, 9, true, Migrations.MIGRATION_8_9)

        val cursor9 = db.query("SELECT COUNT(*) FROM test_profiles")
        cursor9.use {
            assert(it.moveToFirst())
            assertEquals(5, it.getInt(0))
        }
        db.close()

        // Migra v9 → v10
        db = helper.runMigrationsAndValidate(TEST_DB, 10, true, Migrations.MIGRATION_9_10)

        val cursor10 = db.query("SELECT COUNT(*) FROM test_profiles")
        cursor10.use {
            assert(it.moveToFirst())
            assertEquals(5, it.getInt(0))
        }

        // Verifica che tutti i campi siano accessibili
        val cursorFinal = db.query(
            "SELECT profileId, profileName, pingCount, runSpeedTest FROM test_profiles ORDER BY profileId"
        )
        cursorFinal.use {
            var count = 0
            while (it.moveToNext()) {
                count++
                assertEquals(count, it.getInt(it.getColumnIndexOrThrow("profileId")))
                assertEquals("Profile $count", it.getString(it.getColumnIndexOrThrow("profileName")))
                assertEquals(4, it.getInt(it.getColumnIndexOrThrow("pingCount")))
                assertEquals(0, it.getInt(it.getColumnIndexOrThrow("runSpeedTest")))
            }
            assertEquals(5, count)
        }

        db.close()
    }
}

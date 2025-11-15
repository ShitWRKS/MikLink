package com.app.miklink.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.app.miklink.data.db.AppDatabase
import com.app.miklink.data.db.model.ProbeConfig
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test di integrazione per ProbeConfigDao
 * Usa un database Room in-memory per testare le operazioni DAO
 */
@RunWith(RobolectricTestRunner::class)
class ProbeConfigDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProbeConfigDao

    @Before
    fun setup() {
        // Crea un database in-memory per i test
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        dao = db.probeConfigDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * Test 1: upsertSingle (Insert)
     * Verifica che l'inserimento di una nuova ProbeConfig funzioni correttamente
     */
    @Test
    fun `test upsertSingle inserts new probe`() = runTest {
        // Arrange
        val probe = ProbeConfig(
            probeId = 1L,
            name = "Test Probe",
            ipAddress = "192.168.1.1",
            username = "admin",
            password = "password",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )

        // Act
        dao.upsertSingle(probe)

        // Assert - Verifica usando getSingleProbe
        dao.getSingleProbe().test {
            val result = awaitItem()
            assertEquals(probe.probeId, result?.probeId)
            assertEquals(probe.name, result?.name)
            assertEquals(probe.ipAddress, result?.ipAddress)
            assertEquals(probe.username, result?.username)
            assertEquals(probe.testInterface, result?.testInterface)
            assertEquals(probe.isOnline, result?.isOnline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 2: upsertSingle (Update)
     * Verifica che l'update di una ProbeConfig esistente funzioni correttamente
     */
    @Test
    fun `test upsertSingle updates existing probe`() = runTest {
        // Arrange - Inserisce una sonda iniziale
        val originalProbe = ProbeConfig(
            probeId = 1L,
            name = "Original Probe",
            ipAddress = "192.168.1.100",
            username = "admin",
            password = "oldpass",
            testInterface = "ether1",
            isOnline = false,
            modelName = "RB750",
            tdrSupported = false,
            isHttps = false
        )
        dao.upsertSingle(originalProbe)

        // Act - Aggiorna la stessa sonda (stesso probeId)
        val updatedProbe = ProbeConfig(
            probeId = 1L,
            name = "Updated Probe",
            ipAddress = "192.168.1.200",
            username = "newadmin",
            password = "newpass",
            testInterface = "ether2",
            isOnline = true,
            modelName = "CCR1009",
            tdrSupported = true,
            isHttps = true
        )
        dao.upsertSingle(updatedProbe)

        // Assert - Verifica che la sonda sia stata aggiornata
        dao.getSingleProbe().test {
            val result = awaitItem()
            assertEquals(1L, result?.probeId)
            assertEquals("Updated Probe", result?.name)
            assertEquals("192.168.1.200", result?.ipAddress)
            assertEquals("newadmin", result?.username)
            assertEquals("ether2", result?.testInterface)
            assertEquals(true, result?.isOnline)
            assertEquals("CCR1009", result?.modelName)
            assertEquals(true, result?.tdrSupported)
            assertEquals(true, result?.isHttps)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 3: getSingleProbe (Sonda Esistente)
     * Verifica che getSingleProbe restituisca la sonda corretta quando esiste
     */
    @Test
    fun `test getSingleProbe returns existing probe`() = runTest {
        // Arrange
        val probe = ProbeConfig(
            probeId = 5L,
            name = "Test Probe",
            ipAddress = "10.0.0.1",
            username = "user",
            password = "pass",
            testInterface = "wlan1",
            isOnline = true,
            modelName = "hAP",
            tdrSupported = false,
            isHttps = true
        )
        dao.upsertSingle(probe)

        // Act & Assert
        dao.getSingleProbe().test {
            val result = awaitItem()
            assertEquals(probe.probeId, result?.probeId)
            assertEquals(probe.name, result?.name)
            assertEquals(probe.ipAddress, result?.ipAddress)
            assertEquals(probe.testInterface, result?.testInterface)
            assertEquals(probe.modelName, result?.modelName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 4: getSingleProbe (Nessuna Sonda)
     * Verifica che getSingleProbe restituisca null quando il database è vuoto
     */
    @Test
    fun `test getSingleProbe returns null when no probe exists`() = runTest {
        // Arrange - Database vuoto (nessun insert)

        // Act & Assert
        dao.getSingleProbe().test {
            val result = awaitItem()
            assertNull(result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 5: getSingleProbe con più sonde (verifica LIMIT 1)
     * Verifica che getSingleProbe restituisca solo la prima sonda quando ce ne sono multiple
     */
    @Test
    fun `test getSingleProbe returns first probe when multiple exist`() = runTest {
        // Arrange - Inserisce più sonde
        val probe1 = ProbeConfig(
            probeId = 1L,
            name = "First Probe",
            ipAddress = "192.168.1.1",
            username = "admin1",
            password = "pass1",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB750",
            tdrSupported = true,
            isHttps = false
        )
        val probe2 = ProbeConfig(
            probeId = 2L,
            name = "Second Probe",
            ipAddress = "192.168.1.2",
            username = "admin2",
            password = "pass2",
            testInterface = "ether2",
            isOnline = false,
            modelName = "CCR1009",
            tdrSupported = false,
            isHttps = true
        )

        dao.insert(probe1)
        dao.insert(probe2)

        // Act & Assert - Deve restituire la prima sonda (ORDER BY probeId ASC LIMIT 1)
        dao.getSingleProbe().test {
            val result = awaitItem()
            assertEquals(1L, result?.probeId)
            assertEquals("First Probe", result?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}


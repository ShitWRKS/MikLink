package com.app.miklink.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.app.miklink.data.db.AppDatabase
import com.app.miklink.data.db.model.Client
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test di integrazione per ClientDao
 * Usa un database Room in-memory per testare le operazioni CRUD
 */
@RunWith(RobolectricTestRunner::class)
class ClientDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ClientDao

    @Before
    fun setup() {
        // Crea un database in-memory per i test
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        dao = db.clientDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * Test 1: insert
     * Verifica che l'inserimento di un nuovo Client funzioni correttamente
     */
    @Test
    fun `test insert creates new client`() = runTest {
        // Arrange
        val client = Client(
            companyName = "Acme Corp",
            location = "Sede",
            notes = "Cliente Test",
            networkMode = "STATIC",
            staticIp = "192.168.1.100",
            staticSubnet = "255.255.255.0",
            staticGateway = "192.168.1.1",
            staticCidr = "192.168.1.100/24",
            minLinkRate = "1G",
            socketPrefix = "ACM",
            nextIdNumber = 1,
            lastFloor = "Piano 1",
            lastRoom = "Sala A"
        )

        // Act
        dao.insert(client)

        // Assert - Verifica usando getAllClients
        dao.getAllClients().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Acme Corp", result[0].companyName)
            assertEquals("Sede", result[0].location)
            assertEquals("Cliente Test", result[0].notes)
            assertEquals("STATIC", result[0].networkMode)
            assertEquals("192.168.1.100", result[0].staticIp)
            assertEquals("1G", result[0].minLinkRate)
            assertEquals("ACM", result[0].socketPrefix)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 2: update
     * Verifica che l'aggiornamento di un Client esistente funzioni correttamente
     */
    @Test
    fun `test update modifies existing client`() = runTest {
        // Arrange - Inserisce un client iniziale
        val originalClient = Client(
            companyName = "Vecchio Nome",
            location = "Vecchia Sede",
            notes = "Note Vecchie",
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            minLinkRate = "100M",
            socketPrefix = "OLD",
            nextIdNumber = 1
        )
        dao.insert(originalClient)

        // Ottiene il client inserito per avere l'ID auto-generato
        var insertedClient: Client? = null
        dao.getAllClients().test {
            insertedClient = awaitItem().firstOrNull()
            cancelAndIgnoreRemainingEvents()
        }

        assertNotNull(insertedClient)
        insertedClient?.let { clientToUpdate ->
            // Act - Aggiorna il client
            val updatedClient = clientToUpdate.copy(
                companyName = "Nuovo Nome",
                location = "Nuova Sede",
                notes = "Note Nuove",
                networkMode = "STATIC",
                staticIp = "10.0.0.1",
                minLinkRate = "10G",
                socketPrefix = "NEW"
            )
            dao.update(updatedClient)

            // Assert - Verifica che il client sia stato aggiornato
            dao.getAllClients().test {
                val result = awaitItem()
                assertEquals(1, result.size)
                assertEquals("Nuovo Nome", result[0].companyName)
                assertEquals("Nuova Sede", result[0].location)
                assertEquals("Note Nuove", result[0].notes)
                assertEquals("STATIC", result[0].networkMode)
                assertEquals("10.0.0.1", result[0].staticIp)
                assertEquals("10G", result[0].minLinkRate)
                assertEquals("NEW", result[0].socketPrefix)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /**
     * Test 3: delete
     * Verifica che la cancellazione di un Client funzioni correttamente
     */
    @Test
    fun `test delete removes client`() = runTest {
        // Arrange - Inserisce un client
        val client = Client(
            companyName = "Client Da Eliminare",
            location = "Sede",
            notes = null,
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            minLinkRate = "1G",
            socketPrefix = "DEL"
        )
        dao.insert(client)

        // Verifica che il client sia stato inserito
        var insertedClient: Client? = null
        dao.getAllClients().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            insertedClient = result.first()
            cancelAndIgnoreRemainingEvents()
        }

        assertNotNull(insertedClient)
        insertedClient?.let { clientToDelete ->
            // Act - Elimina il client
            dao.delete(clientToDelete)
        }

        // Assert - Verifica che getAllClients sia vuoto
        dao.getAllClients().test {
            val result = awaitItem()
            assertTrue("La lista dovrebbe essere vuota dopo la cancellazione", result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 4: getAllClients (Flow)
     * Verifica che il Flow getAllClients emetta la lista completa dei client
     */
    @Test
    fun `test getAllClients emits list of three clients`() = runTest {
        // Arrange - Inserisce 3 client
        val client1 = Client(
            companyName = "Alpha Corp",
            location = "Milano",
            notes = "Primo cliente",
            networkMode = "STATIC",
            staticIp = "10.0.0.1",
            staticSubnet = "255.255.255.0",
            staticGateway = "10.0.0.254",
            minLinkRate = "1G",
            socketPrefix = "ALP"
        )
        val client2 = Client(
            companyName = "Beta Ltd",
            location = "Roma",
            notes = "Secondo cliente",
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            minLinkRate = "100M",
            socketPrefix = "BET"
        )
        val client3 = Client(
            companyName = "Gamma Inc",
            location = "Torino",
            notes = "Terzo cliente",
            networkMode = "STATIC",
            staticIp = "172.16.0.1",
            staticSubnet = "255.255.0.0",
            staticGateway = "172.16.0.254",
            minLinkRate = "10G",
            socketPrefix = "GAM"
        )

        dao.insert(client1)
        dao.insert(client2)
        dao.insert(client3)

        // Act & Assert - Verifica che il Flow emetta una lista di 3 client
        dao.getAllClients().test {
            val result = awaitItem()
            assertEquals(3, result.size)

            // Verifica l'ordine alfabetico per companyName (come specificato nella query)
            assertEquals("Alpha Corp", result[0].companyName)
            assertEquals("Beta Ltd", result[1].companyName)
            assertEquals("Gamma Inc", result[2].companyName)

            // Verifica alcuni dettagli dei client
            assertEquals("Milano", result[0].location)
            assertEquals("DHCP", result[1].networkMode)
            assertEquals("10G", result[2].minLinkRate)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 5: getClientById
     * Verifica che getClientById ritorni il client corretto
     */
    @Test
    fun `test getClientById returns correct client`() = runTest {
        // Arrange - Inserisce un client
        val client = Client(
            companyName = "Target Client",
            location = "Firenze",
            notes = "Cliente da cercare",
            networkMode = "STATIC",
            staticIp = "192.168.100.1",
            staticSubnet = "255.255.255.0",
            staticGateway = "192.168.100.254",
            minLinkRate = "1G",
            socketPrefix = "TGT",
            nextIdNumber = 5,
            lastFloor = "Piano 2",
            lastRoom = "Stanza B"
        )
        dao.insert(client)

        // Ottiene l'ID auto-generato
        var clientId: Long = 0
        dao.getAllClients().test {
            clientId = awaitItem().first().clientId
            cancelAndIgnoreRemainingEvents()
        }

        // Act & Assert - Cerca il client per ID
        dao.getClientById(clientId).test {
            val result = awaitItem()
            assertEquals(clientId, result?.clientId)
            assertEquals("Target Client", result?.companyName)
            assertEquals("Firenze", result?.location)
            assertEquals("Cliente da cercare", result?.notes)
            assertEquals("STATIC", result?.networkMode)
            assertEquals("192.168.100.1", result?.staticIp)
            assertEquals("1G", result?.minLinkRate)
            assertEquals("TGT", result?.socketPrefix)
            assertEquals(5, result?.nextIdNumber)
            assertEquals("Piano 2", result?.lastFloor)
            assertEquals("Stanza B", result?.lastRoom)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 6: getClientById con ID inesistente
     * Verifica che getClientById ritorni null per un ID inesistente
     */
    @Test
    fun `test getClientById returns null for non-existent id`() = runTest {
        // Act & Assert - Cerca un client con ID inesistente
        dao.getClientById(999L).test {
            val result = awaitItem()
            assertEquals(null, result)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

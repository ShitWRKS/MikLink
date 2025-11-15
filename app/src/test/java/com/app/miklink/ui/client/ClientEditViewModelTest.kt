package com.app.miklink.ui.client

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.NetworkMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test per ClientEditViewModel
 * Testa la logica di caricamento, validazione e salvataggio del client
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientEditViewModelTest {

    private lateinit var clientDao: ClientDao
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        clientDao = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test 1: Modalità Creazione - Init
     * Verifica che il ViewModel si avvii in modalità "creazione" con campi vuoti
     */
    @Test
    fun `test init in creation mode with empty fields`() = runTest {
        // Arrange - SavedStateHandle senza clientId (modalità creazione)
        val savedStateHandle = SavedStateHandle()

        // Act - Crea il ViewModel
        val viewModel = ClientEditViewModel(clientDao, savedStateHandle)

        // Assert - Verifica modalità creazione e campi vuoti
        assertFalse("isEditing dovrebbe essere false in modalità creazione", viewModel.isEditing)
        assertEquals("", viewModel.companyName.value)
        assertEquals("", viewModel.location.value)
        assertEquals("", viewModel.notes.value)
        assertEquals(NetworkMode.DHCP, viewModel.networkMode.value)
        assertEquals("", viewModel.staticIp.value)
        assertEquals("", viewModel.staticSubnet.value)
        assertEquals("", viewModel.staticGateway.value)
        assertEquals("", viewModel.staticCidr.value)
        assertEquals("1G", viewModel.minLinkRate.value) // Valore di default
        assertEquals("", viewModel.socketPrefix.value)
        assertEquals("", viewModel.lastFloor.value)
        assertEquals("", viewModel.lastRoom.value)

        viewModel.isSaved.test {
            assertFalse("isSaved dovrebbe essere false all'init", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 2: Modalità Modifica - Init
     * Verifica che il ViewModel carichi i dati del client esistente
     */
    @Test
    fun `test init in edit mode loads existing client data`() = runTest {
        // Arrange - Client fittizio esistente
        val existingClient = Client(
            clientId = 123L,
            companyName = "Nome Fittizio",
            location = "Milano",
            notes = "Note di test",
            networkMode = NetworkMode.STATIC.name,
            staticIp = "192.168.1.100",
            staticSubnet = "255.255.255.0",
            staticGateway = "192.168.1.1",
            staticCidr = "192.168.1.100/24",
            minLinkRate = "10G",
            socketPrefix = "FIT",
            nextIdNumber = 5,
            lastFloor = "Piano 3",
            lastRoom = "Stanza C"
        )

        // Mock del DAO per ritornare il client esistente
        coEvery { clientDao.getClientById(123L) } returns flowOf(existingClient)

        // SavedStateHandle con clientId (modalità modifica)
        val savedStateHandle = SavedStateHandle(mapOf("clientId" to 123L))

        // Act - Crea il ViewModel (l'init caricherà i dati)
        val viewModel = ClientEditViewModel(clientDao, savedStateHandle)

        // Assert - Verifica modalità modifica e dati caricati
        assertTrue("isEditing dovrebbe essere true in modalità modifica", viewModel.isEditing)

        // Verifica che i campi siano stati popolati con i dati del client
        assertEquals("Nome Fittizio", viewModel.companyName.value)
        assertEquals("Milano", viewModel.location.value)
        assertEquals("Note di test", viewModel.notes.value)
        assertEquals(NetworkMode.STATIC, viewModel.networkMode.value)
        assertEquals("192.168.1.100", viewModel.staticIp.value)
        assertEquals("255.255.255.0", viewModel.staticSubnet.value)
        assertEquals("192.168.1.1", viewModel.staticGateway.value)
        assertEquals("192.168.1.100/24", viewModel.staticCidr.value)
        assertEquals("10G", viewModel.minLinkRate.value)
        assertEquals("FIT", viewModel.socketPrefix.value)
        assertEquals("Piano 3", viewModel.lastFloor.value)
        assertEquals("Stanza C", viewModel.lastRoom.value)

        // Verifica che getClientById sia stato chiamato
        coVerify(exactly = 1) { clientDao.getClientById(123L) }
    }

    /**
     * Test 3: Salvataggio - Validazione Fallita (Nome Vuoto)
     * Verifica che il salvataggio con dati non validi non chiami il DAO
     *
     * Nota: Il ViewModel attuale non ha validazione esplicita.
     * Questo test dimostra il comportamento corrente (insert viene chiamato anche con dati vuoti).
     * In un refactoring futuro, si potrebbe aggiungere validazione e modificare questo test.
     */
    @Test
    fun `test saveClient with empty company name still calls insert`() = runTest {
        // Arrange - SavedStateHandle in modalità creazione
        val savedStateHandle = SavedStateHandle()
        val viewModel = ClientEditViewModel(clientDao, savedStateHandle)

        // Imposta dati non validi (companyName vuoto)
        viewModel.companyName.value = ""
        viewModel.location.value = "Milano"

        // Act - Tenta il salvataggio
        viewModel.saveClient()

        // Assert - Il ViewModel attuale NON ha validazione, quindi insert viene chiamato
        // In un refactoring futuro, questo test dovrebbe verificare che insert NON sia chiamato
        coVerify(exactly = 1) { clientDao.insert(any()) }

        // Verifica che isSaved sia true
        viewModel.isSaved.test {
            assertTrue("isSaved dovrebbe essere true dopo saveClient", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 4: Salvataggio - Successo Creazione
     * Verifica che il salvataggio in modalità creazione chiami insert con i dati corretti
     */
    @Test
    fun `test saveClient in creation mode calls insert with correct data`() = runTest {
        // Arrange - SavedStateHandle in modalità creazione
        val savedStateHandle = SavedStateHandle()
        val viewModel = ClientEditViewModel(clientDao, savedStateHandle)

        // Imposta dati validi
        viewModel.companyName.value = "Nuovo Cliente"
        viewModel.location.value = "Roma"
        viewModel.notes.value = "Cliente appena creato"
        viewModel.networkMode.value = NetworkMode.DHCP
        viewModel.minLinkRate.value = "1G"
        viewModel.socketPrefix.value = "NUO"
        viewModel.lastFloor.value = "Piano 1"
        viewModel.lastRoom.value = "Sala A"

        // Act - Salva il client
        viewModel.saveClient()

        // Assert - Verifica che insert sia stato chiamato con l'oggetto corretto
        coVerify(exactly = 1) {
            clientDao.insert(
                match { client ->
                    client.clientId == 0L && // ID 0 per nuova creazione
                    client.companyName == "Nuovo Cliente" &&
                    client.location == "Roma" &&
                    client.notes == "Cliente appena creato" &&
                    client.networkMode == NetworkMode.DHCP.name &&
                    client.staticIp == null && // DHCP non ha IP statico
                    client.staticSubnet == null &&
                    client.staticGateway == null &&
                    client.staticCidr == null &&
                    client.minLinkRate == "1G" &&
                    client.socketPrefix == "NUO" &&
                    client.nextIdNumber == 1 // Valore di default per nuovo client
                }
            )
        }

        // Verifica che isSaved sia true
        viewModel.isSaved.test {
            assertTrue("isSaved dovrebbe essere true dopo il salvataggio", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 5: Salvataggio - Successo Modifica
     * Verifica che il salvataggio in modalità modifica chiami insert (REPLACE) con i dati aggiornati
     */
    @Test
    fun `test saveClient in edit mode calls insert with updated data`() = runTest {
        // Arrange - Client esistente
        val existingClient = Client(
            clientId = 456L,
            companyName = "Cliente Originale",
            location = "Torino",
            notes = "Note originali",
            networkMode = NetworkMode.DHCP.name,
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "100M",
            socketPrefix = "ORI",
            nextIdNumber = 10,
            lastFloor = "Piano 2",
            lastRoom = "Stanza B"
        )

        // Mock del DAO
        coEvery { clientDao.getClientById(456L) } returns flowOf(existingClient)

        // SavedStateHandle in modalità modifica
        val savedStateHandle = SavedStateHandle(mapOf("clientId" to 456L))
        val viewModel = ClientEditViewModel(clientDao, savedStateHandle)

        // Attende che l'init carichi i dati (l'UnconfinedTestDispatcher esegue immediatamente)
        // I dati sono già caricati grazie all'UnconfinedTestDispatcher

        // Act - Modifica alcuni campi
        viewModel.companyName.value = "Nome Modificato"
        viewModel.networkMode.value = NetworkMode.STATIC
        viewModel.staticIp.value = "10.0.0.50"
        viewModel.staticSubnet.value = "255.255.255.0"
        viewModel.staticGateway.value = "10.0.0.1"
        viewModel.staticCidr.value = "10.0.0.50/24"
        viewModel.minLinkRate.value = "10G"

        // Salva le modifiche
        viewModel.saveClient()

        // Assert - Verifica che insert sia stato chiamato con i dati aggiornati
        coVerify(exactly = 1) {
            clientDao.insert(
                match { client ->
                    client.clientId == 456L && // Stesso ID del client esistente
                    client.companyName == "Nome Modificato" &&
                    client.location == "Torino" && // Campo non modificato
                    client.notes == "Note originali" && // Campo non modificato
                    client.networkMode == NetworkMode.STATIC.name &&
                    client.staticIp == "10.0.0.50" &&
                    client.staticSubnet == "255.255.255.0" &&
                    client.staticGateway == "10.0.0.1" &&
                    client.staticCidr == "10.0.0.50/24" &&
                    client.minLinkRate == "10G" &&
                    client.socketPrefix == "ORI" && // Campo non modificato
                    client.nextIdNumber == 10 // Preservato dall'originale
                }
            )
        }

        // Verifica che isSaved sia true
        viewModel.isSaved.test {
            assertTrue("isSaved dovrebbe essere true dopo l'update", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 6: Modalità Modifica - Client Non Trovato
     * Verifica il comportamento quando il client da modificare non esiste nel DB
     */
    @Test
    fun `test edit mode with non-existent client keeps empty fields`() = runTest {
        // Arrange - Mock del DAO che ritorna null (client non trovato)
        coEvery { clientDao.getClientById(999L) } returns flowOf(null)

        // SavedStateHandle con clientId inesistente
        val savedStateHandle = SavedStateHandle(mapOf("clientId" to 999L))

        // Act - Crea il ViewModel
        val viewModel = ClientEditViewModel(clientDao, savedStateHandle)

        // Assert - Verifica che isEditing sia true ma i campi rimangano vuoti
        assertTrue("isEditing dovrebbe essere true", viewModel.isEditing)
        assertEquals("", viewModel.companyName.value)
        assertEquals("", viewModel.location.value)
        assertEquals("", viewModel.notes.value)

        // Verifica che getClientById sia stato chiamato
        coVerify(exactly = 1) { clientDao.getClientById(999L) }
    }

    /**
     * Test 7: Salvataggio con NetworkMode STATIC
     * Verifica che i campi IP statici siano salvati solo quando networkMode è STATIC
     */
    @Test
    fun `test saveClient with STATIC network mode saves IP fields`() = runTest {
        // Arrange
        val savedStateHandle = SavedStateHandle()
        val viewModel = ClientEditViewModel(clientDao, savedStateHandle)

        // Imposta NetworkMode STATIC con IP
        viewModel.companyName.value = "Cliente Static IP"
        viewModel.networkMode.value = NetworkMode.STATIC
        viewModel.staticIp.value = "172.16.0.100"
        viewModel.staticSubnet.value = "255.255.0.0"
        viewModel.staticGateway.value = "172.16.0.1"
        viewModel.staticCidr.value = "172.16.0.100/16"

        // Act
        viewModel.saveClient()

        // Assert - Verifica che i campi IP siano stati salvati
        coVerify(exactly = 1) {
            clientDao.insert(
                match { client ->
                    client.networkMode == NetworkMode.STATIC.name &&
                    client.staticIp == "172.16.0.100" &&
                    client.staticSubnet == "255.255.0.0" &&
                    client.staticGateway == "172.16.0.1" &&
                    client.staticCidr == "172.16.0.100/16"
                }
            )
        }
    }

    /**
     * Test 8: Salvataggio con NetworkMode DHCP ignora campi IP
     * Verifica che i campi IP statici NON siano salvati quando networkMode è DHCP
     */
    @Test
    fun `test saveClient with DHCP network mode ignores IP fields`() = runTest {
        // Arrange
        val savedStateHandle = SavedStateHandle()
        val viewModel = ClientEditViewModel(clientDao, savedStateHandle)

        // Imposta NetworkMode DHCP (anche se i campi IP sono compilati, devono essere ignorati)
        viewModel.companyName.value = "Cliente DHCP"
        viewModel.networkMode.value = NetworkMode.DHCP
        viewModel.staticIp.value = "192.168.1.100" // Questi campi dovrebbero essere ignorati
        viewModel.staticSubnet.value = "255.255.255.0"
        viewModel.staticGateway.value = "192.168.1.1"
        viewModel.staticCidr.value = "192.168.1.100/24"

        // Act
        viewModel.saveClient()

        // Assert - Verifica che i campi IP siano NULL
        coVerify(exactly = 1) {
            clientDao.insert(
                match { client ->
                    client.networkMode == NetworkMode.DHCP.name &&
                    client.staticIp == null &&
                    client.staticSubnet == null &&
                    client.staticGateway == null &&
                    client.staticCidr == null
                }
            )
        }
    }
}


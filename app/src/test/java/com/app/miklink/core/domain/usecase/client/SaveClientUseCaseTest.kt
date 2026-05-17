/*
 * Purpose: Validate SaveClientUseCase routes new clients to insert and existing ones to update without constraint issues.
 * Inputs: Fake ClientRepository tracking insert/update calls with provided Client ids.
 * Outputs: Assertions on chosen code path and returned identifiers.
 * Notes: Guards the GR3 rule (no insert on edit) at the use case level.
 */
package com.app.miklink.core.domain.usecase.client

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SaveClientUseCaseTest {
    private val fakeRepository = FakeClientRepository()
    private val useCase = SaveClientUseCaseImpl(fakeRepository)

    @Test
    fun `inserts when client id is zero`() = runBlocking {
        val id = useCase(client(clientId = 0))

        assertTrue(fakeRepository.insertCalled)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(42L, id)
    }

    @Test
    fun `updates when client id is present`() = runBlocking {
        val id = useCase(client(clientId = 7))

        assertEquals(7, fakeRepository.updateCalls)
        assertTrue(fakeRepository.insertCalled.not())
        assertEquals(7L, id)
    }

    @Test
    fun `dhcp without static cidr or gateway saves`() = runBlocking {
        val id = useCase(
            client(
                clientId = 0,
                networkMode = NetworkMode.DHCP,
                staticCidr = null,
                staticGateway = null
            )
        )

        assertTrue(fakeRepository.insertCalled)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(42L, id)
    }

    @Test
    fun `static with valid cidr and gateway saves`() = runBlocking {
        val id = useCase(
            client(
                clientId = 0,
                networkMode = NetworkMode.STATIC,
                staticCidr = "10.0.3.100/24",
                staticGateway = "10.0.3.1"
            )
        )

        assertTrue(fakeRepository.insertCalled)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(42L, id)
    }

    @Test
    fun `static with blank cidr fails without saving`() = runBlocking {
        assertValidationFailsWithoutSaving(
            client(
                networkMode = NetworkMode.STATIC,
                staticCidr = "",
                staticGateway = "10.0.3.1"
            ),
            "Static CIDR is required"
        )
    }

    @Test
    fun `static with cidr missing prefix fails without saving`() = runBlocking {
        assertValidationFailsWithoutSaving(
            client(
                networkMode = NetworkMode.STATIC,
                staticCidr = "10.0.3.100",
                staticGateway = "10.0.3.1"
            ),
            "Invalid static CIDR"
        )
    }

    @Test
    fun `static with cidr prefix out of range fails without saving`() = runBlocking {
        assertValidationFailsWithoutSaving(
            client(
                networkMode = NetworkMode.STATIC,
                staticCidr = "10.0.3.100/33",
                staticGateway = "10.0.3.1"
            ),
            "Invalid static CIDR"
        )
    }

    @Test
    fun `static with blank gateway fails without saving`() = runBlocking {
        assertValidationFailsWithoutSaving(
            client(
                networkMode = NetworkMode.STATIC,
                staticCidr = "10.0.3.100/24",
                staticGateway = ""
            ),
            "Static gateway is required"
        )
    }

    @Test
    fun `static with gateway containing cidr fails without saving`() = runBlocking {
        assertValidationFailsWithoutSaving(
            client(
                networkMode = NetworkMode.STATIC,
                staticCidr = "10.0.3.100/24",
                staticGateway = "10.0.3.1/24"
            ),
            "Invalid static gateway"
        )
    }

    @Test
    fun `static with non ip gateway fails without saving`() = runBlocking {
        assertValidationFailsWithoutSaving(
            client(
                networkMode = NetworkMode.STATIC,
                staticCidr = "10.0.3.100/24",
                staticGateway = "not-an-ip"
            ),
            "Invalid static gateway"
        )
    }

    private class FakeClientRepository : ClientRepository {
        var insertCalled = false
        var updateCalls = 0

        override fun observeAllClients(): Flow<List<Client>> = emptyFlow()

        override suspend fun getClient(id: Long): Client? = null

        override suspend fun insertClient(client: Client): Long {
            insertCalled = true
            return 42L
        }

        override suspend fun updateClient(client: Client) {
            updateCalls = client.clientId.toInt()
        }

        override suspend fun deleteClient(client: Client) = Unit
    }

    private suspend fun assertValidationFailsWithoutSaving(client: Client, expectedMessage: String) {
        try {
            useCase(client)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals(expectedMessage, e.message)
        }
        assertFalse(fakeRepository.insertCalled)
        assertEquals(0, fakeRepository.updateCalls)
    }

    private fun client(
        clientId: Long = 0,
        networkMode: NetworkMode = NetworkMode.DHCP,
        staticGateway: String? = null,
        staticCidr: String? = null
    ) = Client(
        clientId = clientId,
        companyName = "ACME",
        location = null,
        notes = null,
        networkMode = networkMode,
        staticIp = null,
        staticSubnet = null,
        staticGateway = staticGateway,
        staticCidr = staticCidr,
        minLinkRate = "1G",
        socketPrefix = "SW",
        socketSuffix = "",
        socketSeparator = "-",
        socketNumberPadding = 3,
        nextIdNumber = 1,
        speedTestServerAddress = null,
        speedTestServerUser = null,
        speedTestServerPassword = null
    )
}

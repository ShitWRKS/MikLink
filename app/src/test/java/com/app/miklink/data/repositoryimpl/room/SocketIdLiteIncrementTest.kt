/*
 * Purpose: Verify SaveTestReportUseCase increments client nextIdNumber for all saved reports and keeps mappings deterministic.
 * Inputs: Fake repositories with configurable Client state and TestReport objects simulating PASS/FAIL outcomes.
 * Outputs: Assertions on updated Client.nextIdNumber to guard socket suggestion freshness after saves.
 * Notes: Increment policy must apply regardless of PASS/FAIL when incrementClientCounter is true.
 */
package com.app.miklink.data.repositoryimpl.room

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCase
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCaseImpl
import com.app.miklink.data.local.room.dao.TestReportDao
import com.app.miklink.data.local.room.entity.TestReportEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SocketIdLiteIncrementTest {

    private class FakeTestReportDao : TestReportDao {
        private val storage = mutableListOf<TestReportEntity>()
        private var nextId = 1L

        override fun observeAll() = throw NotImplementedError()
        override fun observeByClient(clientId: Long) = throw NotImplementedError()
        override suspend fun getById(id: Long): TestReportEntity? = storage.firstOrNull { it.reportId == id }
        override suspend fun insert(report: TestReportEntity): Long {
            val id = nextId++
            storage.add(report.copy(reportId = id))
            return id
        }

        override suspend fun delete(report: TestReportEntity) {
            storage.removeIf { it.reportId == report.reportId }
        }
    }

    private class FakeClientRepository(initial: List<Client>) : ClientRepository {
        private val map = initial.associateBy { it.clientId }.toMutableMap()

        override fun observeAllClients() = throw NotImplementedError()
        override suspend fun getClient(id: Long) = map[id]
        override suspend fun insertClient(client: Client): Long {
            map[client.clientId] = client
            return client.clientId
        }

        override suspend fun updateClient(client: Client) {
            map[client.clientId] = client
        }

        override suspend fun deleteClient(client: Client) {
            map.remove(client.clientId)
        }
    }

    private fun makeUseCase(client: Client): Pair<SaveTestReportUseCase, FakeClientRepository> {
        val fakeDao = FakeTestReportDao()
        val fakeClientRepo = FakeClientRepository(listOf(client))
        val reportRepository = RoomReportRepository(fakeDao)
        val useCase = SaveTestReportUseCaseImpl(reportRepository, fakeClientRepo)
        return useCase to fakeClientRepo
    }

    @Test
    fun `SUCCESS increments nextIdNumber`() = runBlocking {
        val client = Client(
            clientId = 1L,
            companyName = "X",
            location = null,
            notes = null,
            networkMode = com.app.miklink.core.domain.model.NetworkMode.DHCP,
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "",
            socketPrefix = "P",
            socketSuffix = "S",
            socketSeparator = "-",
            socketNumberPadding = 2,
            nextIdNumber = 5,
            speedTestServerAddress = null,
            speedTestServerUser = null,
            speedTestServerPassword = null
        )

        val (useCase, fakeClientRepo) = makeUseCase(client)

        val report = TestReport(
            clientId = client.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = "P-05-S",
            notes = null,
            probeName = null,
            profileName = null,
            overallStatus = "PASS",
            resultsJson = "{}"
        )

        useCase(report)

        val updated = fakeClientRepo.getClient(client.clientId)!!
        assertEquals(6, updated.nextIdNumber)
    }

    @Test
    fun `FAIL also increments`() = runBlocking {
        val client = Client(
            clientId = 2L,
            companyName = "Y",
            location = null,
            notes = null,
            networkMode = com.app.miklink.core.domain.model.NetworkMode.DHCP,
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "",
            socketPrefix = "P",
            socketSuffix = "S",
            socketSeparator = "-",
            socketNumberPadding = 2,
            nextIdNumber = 10,
            speedTestServerAddress = null,
            speedTestServerUser = null,
            speedTestServerPassword = null
        )

        val (useCase, fakeClientRepo) = makeUseCase(client)

        val report = TestReport(
            clientId = client.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = "P-10-S",
            notes = null,
            probeName = null,
            profileName = null,
            overallStatus = "FAIL",
            resultsJson = "{}"
        )

        useCase(report)

        val updated = fakeClientRepo.getClient(client.clientId)!!
        assertEquals(11, updated.nextIdNumber)
    }

    @Test
    fun `Multiple SUCCESS increments monotonically`() = runBlocking {
        val client = Client(
            clientId = 3L,
            companyName = "Z",
            location = null,
            notes = null,
            networkMode = com.app.miklink.core.domain.model.NetworkMode.DHCP,
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "",
            socketPrefix = "P",
            socketSuffix = "S",
            socketSeparator = "-",
            socketNumberPadding = 2,
            nextIdNumber = 1,
            speedTestServerAddress = null,
            speedTestServerUser = null,
            speedTestServerPassword = null
        )

        val (useCase, fakeClientRepo) = makeUseCase(client)

        val report1 = TestReport(
            clientId = client.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = "P-01-S",
            notes = null,
            probeName = null,
            profileName = null,
            overallStatus = "PASS",
            resultsJson = "{}"
        )

        val report2 = report1.copy(timestamp = System.currentTimeMillis() + 1000)

        useCase(report1)
        useCase(report2)

        val updated = fakeClientRepo.getClient(client.clientId)!!
        assertEquals(3, updated.nextIdNumber)
    }
}

package com.app.miklink.e2e.support

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestFixtureManagerTest {
    @Test
    fun cleanupDeletesOnlyOwnedRecordsAndIsIdempotent() = runBlocking {
        val clients = FakeClientRepository()
        val profiles = FakeProfileRepository()
        val reports = FakeReportRepository()
        val unrelatedId = clients.insertClient(TestFixtureManager.defaultClient("unrelated"))
        val manager = TestFixtureManager("session-123", clients, profiles, reports)

        val fixtures = manager.createCoreFixtures()
        manager.cleanup()
        manager.cleanup()

        assertTrue(fixtures.client.companyName.startsWith("e2e-session-123"))
        assertEquals(null, clients.getClient(fixtures.client.clientId))
        assertTrue(clients.getClient(unrelatedId) != null)
        assertEquals(CleanupStatus.PASS, manager.cleanupResult.status)
    }
}

private class FakeClientRepository : ClientRepository {
    private val state = MutableStateFlow<List<Client>>(emptyList())
    private var next = 1L
    override fun observeAllClients(): Flow<List<Client>> = state
    override suspend fun getClient(id: Long): Client? = state.value.firstOrNull { it.clientId == id }
    override suspend fun insertClient(client: Client): Long = next++.also { id -> state.value += client.copy(clientId = id) }
    override suspend fun updateClient(client: Client) { state.value = state.value.map { if (it.clientId == client.clientId) client else it } }
    override suspend fun deleteClient(client: Client) { state.value = state.value.filterNot { it.clientId == client.clientId } }
    override suspend fun incrementNextIdNumber(clientId: Long): Int = if (getClient(clientId) == null) 0 else 1
}

private class FakeProfileRepository : TestProfileRepository {
    private val state = MutableStateFlow<List<TestProfile>>(emptyList())
    private var next = 1L
    override fun observeAllProfiles(): Flow<List<TestProfile>> = state
    override suspend fun getProfile(id: Long): TestProfile? = state.value.firstOrNull { it.profileId == id }
    override suspend fun insertProfile(profile: TestProfile): Long = next++.also { id -> state.value += profile.copy(profileId = id) }
    override suspend fun updateProfile(profile: TestProfile) { state.value = state.value.map { if (it.profileId == profile.profileId) profile else it } }
    override suspend fun deleteProfile(profile: TestProfile) { state.value = state.value.filterNot { it.profileId == profile.profileId } }
}

private class FakeReportRepository : ReportRepository {
    private val state = MutableStateFlow<List<TestReport>>(emptyList())
    private var next = 1L
    override fun observeAllReports(): Flow<List<TestReport>> = state
    override fun observeReportsByClient(clientId: Long): Flow<List<TestReport>> = state.map { rows -> rows.filter { it.clientId == clientId } }
    override suspend fun getReport(id: Long): TestReport? = state.value.firstOrNull { it.reportId == id }
    override suspend fun saveReport(report: TestReport): Long = next++.also { id -> state.value += report.copy(reportId = id) }
    override suspend fun deleteReport(report: TestReport) { state.value = state.value.filterNot { it.reportId == report.reportId } }
}

package com.app.miklink.data.repository

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TdrCapability
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestReport
import com.squareup.moshi.Moshi
import com.app.miklink.testsupport.TestMoshiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackupManagerTest {

    class FakeTxRunner : com.app.miklink.core.data.transaction.TransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
    }

    class FakeProbeRepo(var probe: ProbeConfig?) : ProbeRepository {
        override suspend fun getProbeConfig(): ProbeConfig? = probe
        override suspend fun saveProbeConfig(config: ProbeConfig) { probe = config }
        override fun observeProbeConfig(): Flow<ProbeConfig?> = flowOf(probe)
    }

    class FakeProfileRepo(initial: List<TestProfile>) : TestProfileRepository {
        private val store = MutableStateFlow(initial.toList())
        private var nextId = 1L
        override fun observeAllProfiles() = store
        override suspend fun getProfile(id: Long): TestProfile? = store.value.find { it.profileId == id }
        override suspend fun insertProfile(profile: TestProfile): Long {
            val toInsert = profile.copy(profileId = nextId++)
            store.value = store.value + toInsert
            return toInsert.profileId
        }
        override suspend fun updateProfile(profile: TestProfile) { store.value = store.value.map { if (it.profileId == profile.profileId) profile else it } }
        override suspend fun deleteProfile(profile: TestProfile) { store.value = store.value.filter { it.profileId != profile.profileId } }
    }

    class FakeClientRepo(initial: List<Client>) : ClientRepository {
        private val store = MutableStateFlow(initial.toList())
        private var nextId = 1L
        override fun observeAllClients() = store
        override suspend fun getClient(id: Long): Client? = store.value.find { it.clientId == id }
        override suspend fun insertClient(client: Client): Long {
            val toInsert = client.copy(clientId = nextId++)
            store.value = store.value + toInsert
            return toInsert.clientId
        }
        override suspend fun updateClient(client: Client) { store.value = store.value.map { if (it.clientId == client.clientId) client else it } }
        override suspend fun deleteClient(client: Client) { store.value = store.value.filter { it.clientId != client.clientId } }
        override suspend fun incrementNextIdNumber(clientId: Long): Int {
            val current = store.value.firstOrNull { it.clientId == clientId } ?: return 0
            store.value = store.value.map {
                if (it.clientId == clientId) it.copy(nextIdNumber = it.nextIdNumber + 1) else it
            }
            return 1
        }
    }

    class FakeReportRepo(initial: List<TestReport>) : ReportRepository {
        private val store = MutableStateFlow(initial.toList())
        private var nextId = 1L
        override fun observeAllReports() = store
        override fun observeReportsByClient(clientId: Long): Flow<List<TestReport>> = flowOf(store.value.filter { it.clientId == clientId })
        override suspend fun getReport(id: Long): TestReport? = store.value.find { it.reportId == id }
        override suspend fun saveReport(report: TestReport): Long {
            val toInsert = report.copy(reportId = nextId++)
            store.value = store.value + toInsert
            return toInsert.reportId
        }
        override suspend fun deleteReport(report: TestReport) { store.value = store.value.filter { it.reportId != report.reportId } }
    }

    @Test
    fun export_includes_single_probe() = runBlocking {
        val probe = ProbeConfig(ipAddress = "1.2.3.4", username = "u", password = "p", testInterface = "eth0", isHttps = false, isOnline = true, modelName = null, tdrCapability = TdrCapability.UNKNOWN)
        val probeRepo = FakeProbeRepo(probe)
        val profileRepo = FakeProfileRepo(emptyList())
        val clientRepo = FakeClientRepo(emptyList())
        val reportRepo = FakeReportRepo(emptyList())
        val manager = BackupManagerImpl(probeRepo, profileRepo, clientRepo, reportRepo, TestMoshiProvider.provideMoshi(), FakeTxRunner())

        val json = manager.exportConfigToJson()
        val adapter = TestMoshiProvider.provideMoshi().adapter(BackupData::class.java)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertNotNull(parsed?.probe)
    }

    @Test
    fun import_with_probe_restores_singleton() = runBlocking {
        val initialProbe = ProbeConfig(ipAddress = "0.0.0.0", username = "a", password = "x", testInterface = "eth0", isHttps = false, isOnline = true, modelName = null, tdrCapability = TdrCapability.UNKNOWN)
        val probeRepo = FakeProbeRepo(initialProbe)
        val profileRepo = FakeProfileRepo(emptyList())
        val clientRepo = FakeClientRepo(emptyList())
        val reportRepo = FakeReportRepo(emptyList())
        val manager = BackupManagerImpl(probeRepo, profileRepo, clientRepo, reportRepo, TestMoshiProvider.provideMoshi(), FakeTxRunner())

        val newProbe = ProbeConfig(ipAddress = "9.9.9.9", username = "new", password = "p", testInterface = "eth1", isHttps = true, isOnline = false, modelName = "m", tdrCapability = TdrCapability.SUPPORTED)
        val backup = BackupData(probe = newProbe, clients = emptyList(), profiles = emptyList(), reports = emptyList())
        val res = manager.importBackupData(backup)
        assertTrue(res.isSuccess)
        assertEquals(newProbe, probeRepo.probe)
    }

    @Test
    fun import_with_null_probe_does_not_crash() = runBlocking {
        val initialProbe = ProbeConfig(ipAddress = "0.0.0.0", username = "a", password = "x", testInterface = "eth0", isHttps = false, isOnline = true, modelName = null, tdrCapability = TdrCapability.UNKNOWN)
        val probeRepo = FakeProbeRepo(initialProbe)
        val profileRepo = FakeProfileRepo(emptyList())
        val clientRepo = FakeClientRepo(emptyList())
        val reportRepo = FakeReportRepo(emptyList())
        val manager = BackupManagerImpl(probeRepo, profileRepo, clientRepo, reportRepo, TestMoshiProvider.provideMoshi(), FakeTxRunner())

        val backup = BackupData(probe = null, clients = emptyList(), profiles = emptyList(), reports = emptyList())
        val res = manager.importBackupData(backup)
        assertTrue(res.isSuccess)
        // Probe should be unchanged
        assertEquals(initialProbe, probeRepo.probe)
    }

    @Test
    fun roundtrip_export_import_preserves_client_report_associations() = runBlocking {
        // initial client with id 42 and a report linked to it
        val client = Client(
            clientId = 42L,
            companyName = "Acme Ltd",
            location = "Rome",
            notes = null,
            networkMode = NetworkMode.DHCP,
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "10",
            socketPrefix = "S",
            socketSuffix = "X",
            socketSeparator = "-",
            socketNumberPadding = 3,
            nextIdNumber = 1,
            speedTestServerAddress = null,
            speedTestServerUser = null,
            speedTestServerPassword = null
        )

        val report = TestReport(
            reportId = 1L,
            clientId = client.clientId,
            timestamp = 123L,
            socketName = "S-001-X",
            notes = null,
            probeName = null,
            profileName = null,
            overallStatus = "OK",
            resultFormatVersion = 1,
            resultsJson = "{\"r\":1}"
        )

        val probeRepo = FakeProbeRepo(null)
        val profileRepo = FakeProfileRepo(emptyList())
        val clientRepo = FakeClientRepo(listOf(client))
        val reportRepo = FakeReportRepo(listOf(report))
        val manager = BackupManagerImpl(probeRepo, profileRepo, clientRepo, reportRepo, TestMoshiProvider.provideMoshi(), FakeTxRunner())

        // Export
        val json = manager.exportConfigToJson()
        val adapter = TestMoshiProvider.provideMoshi().adapter(BackupData::class.java)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(2, parsed!!.version)
        val exportedClientRef = parsed.clients.single().clientRef
        assertTrue(exportedClientRef.isNotBlank())
        // Clear repos to simulate fresh import target
        val emptyClientRepo = FakeClientRepo(emptyList())
        val emptyProfileRepo = FakeProfileRepo(emptyList())
        val emptyReportRepo = FakeReportRepo(emptyList())
        val targetManager = BackupManagerImpl(probeRepo, emptyProfileRepo, emptyClientRepo, emptyReportRepo, TestMoshiProvider.provideMoshi(), FakeTxRunner())

        // Import
        val res = targetManager.importBackupData(parsed)
        assertTrue(res.isSuccess)

        // After import we should have one client and one report, with report.clientId pointing to the new client id
        val clientsAfter = emptyClientRepo.observeAllClients().first()
        val reportsAfter = emptyReportRepo.observeAllReports().first()
        assertEquals(1, clientsAfter.size)
        assertEquals(1, reportsAfter.size)
        val newClient = clientsAfter.single()
        val newReport = reportsAfter.single()
        assertEquals(newClient.clientId, newReport.clientId)
        // clientRef normalized should match
        fun keyOf(c: Client) = c.companyName.trim().lowercase().replace("\\s+".toRegex(), "_") + "|" + (c.location ?: "").trim().lowercase().replace("\\s+".toRegex(), "_")
        assertEquals(keyOf(newClient), exportedClientRef)
    }
}

/*
 * Purpose: Verify dashboard socket suggestions stay fresh when client counters change after a save.
 * Inputs: Fake repositories emitting Client lists and IdNumberingStrategy combined by DashboardViewModel.
 * Outputs: Assertions that socketName recomputes after nextIdNumber increments for the selected client.
 * Notes: Guards against stale selections after Save on PASS or FAIL.
 */
package com.app.miklink.ui.dashboard

import com.app.miklink.core.data.repository.ProbeStatusInfo
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.probe.ProbeStatusRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.model.socketNameFor
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import com.app.miklink.core.domain.usecase.preferences.ObserveIdNumberingStrategyUseCase
import com.app.miklink.testsupport.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `socket suggestion updates after counter increment`() = runTest {
        val client = baseClient(nextId = 1)
        val clientsFlow = MutableStateFlow(listOf(client))
        val viewModel = DashboardViewModel(
            clientRepository = FakeClientRepository(clientsFlow),
            testProfileRepository = FakeTestProfileRepository(),
            reportRepository = FakeReportRepository(),
            probeRepository = FakeProbeRepository(),
            probeStatusRepository = FakeProbeStatusRepository(),
            userPreferencesRepository = FakeUserPreferencesRepository(),
            observeIdNumberingStrategyUseCase = FakeIdNumberingStrategyUseCase()
        )

        viewModel.onClientSelected(client)
        advanceUntilIdle()
        assertEquals(client.socketNameFor(1), viewModel.socketName.value)

        val updated = client.copy(nextIdNumber = 2)
        clientsFlow.value = listOf(updated)

        advanceUntilIdle()
        assertEquals(updated.socketNameFor(2), viewModel.socketName.value)
    }

    private fun baseClient(nextId: Int) = Client(
        clientId = 1L,
        companyName = "Acme",
        location = null,
        notes = null,
        networkMode = NetworkMode.DHCP,
        staticIp = null,
        staticSubnet = null,
        staticGateway = null,
        staticCidr = null,
        minLinkRate = "",
        socketPrefix = "PT",
        socketSuffix = "",
        socketSeparator = "-",
        socketNumberPadding = 2,
        nextIdNumber = nextId,
        speedTestServerAddress = null,
        speedTestServerUser = null,
        speedTestServerPassword = null
    )

    private class FakeClientRepository(
        private val flow: MutableStateFlow<List<Client>>
    ) : ClientRepository {
        override fun observeAllClients(): Flow<List<Client>> = flow
        override suspend fun getClient(id: Long): Client? = flow.value.firstOrNull { it.clientId == id }
        override suspend fun insertClient(client: Client): Long {
            flow.value = flow.value + client
            return client.clientId
        }
        override suspend fun updateClient(client: Client) {
            flow.value = flow.value.map { if (it.clientId == client.clientId) client else it }
        }
        override suspend fun deleteClient(client: Client) {
            flow.value = flow.value.filterNot { it.clientId == client.clientId }
        }
    }

    private class FakeTestProfileRepository : TestProfileRepository {
        private val profiles = MutableStateFlow<List<TestProfile>>(emptyList())
        override fun observeAllProfiles(): Flow<List<TestProfile>> = profiles
        override suspend fun getProfile(id: Long): TestProfile? = profiles.value.firstOrNull { it.profileId == id }
        override suspend fun insertProfile(profile: TestProfile): Long = profile.profileId
        override suspend fun updateProfile(profile: TestProfile) {}
        override suspend fun deleteProfile(profile: TestProfile) {}
    }

    private class FakeReportRepository : ReportRepository {
        private val reports = MutableStateFlow<List<TestReport>>(emptyList())
        override fun observeAllReports(): Flow<List<TestReport>> = reports
        override fun observeReportsByClient(clientId: Long): Flow<List<TestReport>> = reports
        override suspend fun getReport(id: Long): TestReport? = reports.value.firstOrNull { it.reportId == id }
        override suspend fun saveReport(report: TestReport): Long {
            reports.value = reports.value + report
            return report.reportId
        }
        override suspend fun deleteReport(report: TestReport) {
            reports.value = reports.value.filterNot { it.reportId == report.reportId }
        }
    }

    private class FakeProbeRepository : ProbeRepository {
        private val probe = MutableStateFlow<ProbeConfig?>(null)
        override fun observeProbeConfig(): Flow<ProbeConfig?> = probe
        override suspend fun getProbeConfig(): ProbeConfig? = probe.value
        override suspend fun saveProbeConfig(config: ProbeConfig) {
            probe.value = config
        }
    }

    private class FakeProbeStatusRepository : ProbeStatusRepository {
        override fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean> = flowOf(false)
        override fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>> = flowOf(emptyList())
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        override val idNumberingStrategy: Flow<IdNumberingStrategy> = flowOf(IdNumberingStrategy.CONTINUOUS_INCREMENT)
        override val pdfIncludeEmptyTests: Flow<Boolean> = flowOf(false)
        override val pdfSelectedColumns: Flow<Set<String>> = flowOf(emptySet())
        override val pdfReportTitle: Flow<String> = flowOf("")
        override val pdfHideEmptyColumns: Flow<Boolean> = flowOf(false)
        override val dashboardGlowIntensity: Flow<Float> = flowOf(0.5f)
        override val probePollingInterval: Flow<Long> = flowOf(5000)
        override val neighborDiscoveryProtocols: Flow<Set<String>> = flowOf(setOf("CDP", "LLDP", "MNDP"))
        override suspend fun setIdNumberingStrategy(strategy: IdNumberingStrategy) {}
        override suspend fun setPdfIncludeEmptyTests(include: Boolean) {}
        override suspend fun setPdfSelectedColumns(columns: Set<String>) {}
        override suspend fun setPdfReportTitle(title: String) {}
        override suspend fun setPdfHideEmptyColumns(hide: Boolean) {}
        override suspend fun setDashboardGlowIntensity(intensity: Float) {}
        override suspend fun setProbePollingInterval(interval: Long) {}
        override suspend fun setNeighborDiscoveryProtocols(protocols: Set<String>) {}
        override suspend fun resetPdfPreferencesToDefaults() {}
    }

    private class FakeIdNumberingStrategyUseCase(
        private val strategy: IdNumberingStrategy = IdNumberingStrategy.CONTINUOUS_INCREMENT
    ) : ObserveIdNumberingStrategyUseCase {
        override fun invoke(): Flow<IdNumberingStrategy> = flowOf(strategy)
    }
}

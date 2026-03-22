package com.app.miklink.core.domain.usecase.test

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.step.CableTestStep
import com.app.miklink.core.domain.test.step.LinkStatusStep
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import com.app.miklink.core.domain.test.step.PingStep
import com.app.miklink.core.domain.test.step.SpeedTestStep
import com.app.miklink.core.data.report.ReportResultsCodec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunTestUseCaseImplTest {

    private val clientRepository: ClientRepository = mockk()
    private val probeRepository: ProbeRepository = mockk()
    private val profileRepository: TestProfileRepository = mockk()

    private val reportResultsCodec: ReportResultsCodec = mockk()
    private val context: android.content.Context = mockk()

    private val networkStep = object : NetworkConfigStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<NetworkConfigFeedback> {
            return StepResult.Success(
                NetworkConfigFeedback(
                    mode = "dhcp",
                    interfaceName = "ether1",
                    address = "10.0.0.2",
                    gateway = "10.0.0.1",
                    dns = "8.8.8.8",
                    message = "OK"
                )
            )
        }
    }

    private val linkStatusStep = object : LinkStatusStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<LinkStatusData> {
            return StepResult.Success(LinkStatusData(status = "up", rate = "1G"))
        }
    }

    private val cableTestStep = object : CableTestStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<CableTestSummary> {
            return StepResult.Success(CableTestSummary(status = "ok", entries = emptyList()))
        }
    }

    private val neighborStep = object : NeighborDiscoveryStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<NeighborData>> {
            return StepResult.Success(
                listOf(
                    NeighborData(
                        identity = "Switch-1",
                        interfaceName = "ether1",
                        discoveredBy = "LLDP",
                        vlanId = null,
                        voiceVlanId = null,
                        poeClass = null,
                        systemDescription = null,
                        portId = null
                    )
                )
            )
        }
    }

    private val pingStep = object : PingStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<PingTargetOutcome>> {
            return StepResult.Success(
                listOf(
                    PingTargetOutcome(
                        target = "8.8.8.8",
                        resolved = "8.8.8.8",
                        packetLoss = "0",
                        results = emptyList<PingMeasurement>(),
                        error = null
                    )
                )
            )
        }
    }

    private val speedTestStep = object : SpeedTestStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<SpeedTestData> {
            return StepResult.Success(
                SpeedTestData(
                    status = "ok",
                    ping = "1/2/3",
                    jitter = "1/2/3",
                    loss = "0",
                    tcpDownload = "900",
                    tcpUpload = "900",
                    udpDownload = "800",
                    udpUpload = "800",
                    warning = null,
                    serverAddress = null
                )
            )
        }
    }

    private val useCase = RunTestUseCaseImpl(
        context = context,
        clientRepository = clientRepository,
        probeRepository = probeRepository,
        testProfileRepository = profileRepository,
        networkConfigStep = networkStep,
        linkStatusStep = linkStatusStep,
        cableTestStep = cableTestStep,
        neighborDiscoveryStep = neighborStep,
        pingStep = pingStep,
        speedTestStep = speedTestStep,
        reportResultsCodec = reportResultsCodec
    )

    @Test
    fun `execute emits live sections updates with deterministic order`() = runTest {
        val client = Client(
            clientId = 1,
            companyName = "Acme",
            location = "HQ",
            notes = null,
            networkMode = NetworkMode.DHCP,
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "1G",
            socketPrefix = "",
            socketSuffix = "",
            socketSeparator = "",
            socketNumberPadding = 3,
            nextIdNumber = 1,
            speedTestServerAddress = "speed.example.com",
            speedTestServerUser = null,
            speedTestServerPassword = null
        )
        val probe = ProbeConfig(
            ipAddress = "10.0.0.10",
            username = "admin",
            password = "admin",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB",
            tdrSupported = true,
            isHttps = false
        )
        val profile = TestProfile(
            profileId = 1,
            profileName = "Default",
            profileDescription = null,
            runTdr = true,
            runLinkStatus = true,
            runLldp = true,
            runPing = true,
            pingTarget1 = "8.8.8.8",
            pingTarget2 = null,
            pingTarget3 = null,
            pingCount = 4,
            runSpeedTest = true,
            thresholds = com.app.miklink.core.domain.model.TestThresholds.defaults()
        )

        coEvery { clientRepository.getClient(1) } returns client
        coEvery { probeRepository.getProbeConfig() } returns probe
        coEvery { profileRepository.getProfile(1) } returns profile
        every { reportResultsCodec.encode(any()) } returns Result.success("{}")
        every { context.getString(any(), *anyVararg()) } returns "log message"
        every { context.getString(any()) } returns "log message"

        val plan = TestPlan(
            clientId = 1,
            profileId = 1,
            socketId = "A1",
            notes = null
        )

        val events = useCase.execute(plan).toList()
        val snapshotUpdates = events.filterIsInstance<TestEvent.SnapshotUpdated>()
        assertTrue("Expected typed snapshot updates", snapshotUpdates.isNotEmpty())

        val firstSnapshot = snapshotUpdates.first().snapshot
        val expectedOrder = listOf("LINK", "NETWORK", "TDR", "NEIGHBORS", "PING", "SPEED")
        val actualOrder = firstSnapshot.sections.map { it.id.name }
        assertEquals(expectedOrder, actualOrder)
        firstSnapshot.sections.forEach { section ->
            org.junit.Assert.assertEquals(
                com.app.miklink.core.domain.test.model.TestSectionStatus.PENDING,
                section.status
            )
        }

        val completed = events.lastOrNull { it is TestEvent.Completed } as? TestEvent.Completed
        assertTrue("Completed event should be emitted", completed != null)
        completed?.let { event ->
            assertEquals("PASS", event.outcome.overallStatus)
            assertEquals(
                com.app.miklink.core.domain.test.model.TestProgressKey.COMPLETED,
                event.outcome.finalSnapshot.progress
            )
            assertTrue("rawResultsJson should be present", !event.outcome.rawResultsJson.isNullOrBlank())
        }
    }

    @Test
    fun `execute keeps network and speed results when ping fails`() = runTest {
        val client = defaultClient()
        val probe = defaultProbe()
        val profile = defaultProfile()

        coEvery { clientRepository.getClient(1) } returns client
        coEvery { probeRepository.getProbeConfig() } returns probe
        coEvery { profileRepository.getProfile(1) } returns profile
        every { reportResultsCodec.encode(any()) } returns Result.success("{}")
        every { context.getString(any(), *anyVararg()) } returns "log message"
        every { context.getString(any()) } returns "log message"

        val failingPingStep = object : PingStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<PingTargetOutcome>> {
                return StepResult.Success(
                    listOf(
                        PingTargetOutcome(
                            target = "8.8.8.8",
                            resolved = "8.8.8.8",
                            packetLoss = null,
                            results = emptyList(),
                            error = "target unreachable"
                        )
                    )
                )
            }
        }

        val successfulSpeedStep = object : SpeedTestStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<SpeedTestData> {
                return StepResult.Success(
                    SpeedTestData(
                        status = "done",
                        ping = "10/12/14ms",
                        jitter = "1/2/3ms",
                        loss = "0%",
                        tcpDownload = "900Mbps",
                        tcpUpload = "800Mbps",
                        udpDownload = "850Mbps",
                        udpUpload = "780Mbps",
                        warning = null,
                        serverAddress = "speed.example.com"
                    )
                )
            }
        }

        val useCase = RunTestUseCaseImpl(
            context = context,
            clientRepository = clientRepository,
            probeRepository = probeRepository,
            testProfileRepository = profileRepository,
            networkConfigStep = networkStep,
            linkStatusStep = linkStatusStep,
            cableTestStep = cableTestStep,
            neighborDiscoveryStep = neighborStep,
            pingStep = failingPingStep,
            speedTestStep = successfulSpeedStep,
            reportResultsCodec = reportResultsCodec
        )

        val plan = TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)
        val events = useCase.execute(plan).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed
        val sections = completed.outcome.finalSnapshot.sections.associateBy { it.id }

        assertEquals(com.app.miklink.core.domain.test.model.TestSectionStatus.PASS, sections[com.app.miklink.core.domain.test.model.TestSectionId.NETWORK]?.status)
        assertEquals(com.app.miklink.core.domain.test.model.TestSectionStatus.FAIL, sections[com.app.miklink.core.domain.test.model.TestSectionId.PING]?.status)
        assertEquals(com.app.miklink.core.domain.test.model.TestSectionStatus.PASS, sections[com.app.miklink.core.domain.test.model.TestSectionId.SPEED]?.status)
        assertTrue(sections[com.app.miklink.core.domain.test.model.TestSectionId.SPEED]?.payload is com.app.miklink.core.domain.test.model.TestSectionPayload.Speed)
    }

    @Test
    fun `execute preserves speed data in report payload`() = runTest {
        val captured = slot<com.app.miklink.core.domain.model.report.ReportData>()

        coEvery { clientRepository.getClient(1) } returns defaultClient()
        coEvery { probeRepository.getProbeConfig() } returns defaultProbe()
        coEvery { profileRepository.getProfile(1) } returns defaultProfile()
        every { reportResultsCodec.encode(capture(captured)) } returns Result.success("{}")
        every { context.getString(any(), *anyVararg()) } returns "log message"
        every { context.getString(any()) } returns "log message"

        val plan = TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)
        useCase.execute(plan).toList()

        assertNotNull(captured.captured.speedTest)
        assertEquals("900", captured.captured.speedTest?.tcpDownload)
    }

    @Test
    fun `execute rethrows cancellation instead of emitting failed event`() = runTest {
        val client = defaultClient()
        val probe = defaultProbe()
        val profile = defaultProfile()

        coEvery { clientRepository.getClient(1) } returns client
        coEvery { probeRepository.getProbeConfig() } returns probe
        coEvery { profileRepository.getProfile(1) } returns profile
        every { reportResultsCodec.encode(any()) } returns Result.success("{}")
        every { context.getString(any(), *anyVararg()) } returns "log message"
        every { context.getString(any()) } returns "log message"

        val cancellingPingStep = object : PingStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<PingTargetOutcome>> {
                throw CancellationException("screen disposed")
            }
        }

        val useCase = RunTestUseCaseImpl(
            context = context,
            clientRepository = clientRepository,
            probeRepository = probeRepository,
            testProfileRepository = profileRepository,
            networkConfigStep = networkStep,
            linkStatusStep = linkStatusStep,
            cableTestStep = cableTestStep,
            neighborDiscoveryStep = neighborStep,
            pingStep = cancellingPingStep,
            speedTestStep = speedTestStep,
            reportResultsCodec = reportResultsCodec
        )

        val plan = TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)
        val result = runCatching { useCase.execute(plan).toList() }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    private fun defaultClient(): Client = Client(
        clientId = 1,
        companyName = "Acme",
        location = "HQ",
        notes = null,
        networkMode = NetworkMode.DHCP,
        staticIp = null,
        staticSubnet = null,
        staticGateway = null,
        staticCidr = null,
        minLinkRate = "1G",
        socketPrefix = "",
        socketSuffix = "",
        socketSeparator = "",
        socketNumberPadding = 3,
        nextIdNumber = 1,
        speedTestServerAddress = "speed.example.com",
        speedTestServerUser = null,
        speedTestServerPassword = null
    )

    private fun defaultProbe(): ProbeConfig = ProbeConfig(
        ipAddress = "10.0.0.10",
        username = "admin",
        password = "admin",
        testInterface = "ether1",
        isOnline = true,
        modelName = "RB",
        tdrSupported = true,
        isHttps = false
    )

    private fun defaultProfile(): TestProfile = TestProfile(
        profileId = 1,
        profileName = "Default",
        profileDescription = null,
        runTdr = true,
        runLinkStatus = true,
        runLldp = true,
        runPing = true,
        pingTarget1 = "8.8.8.8",
        pingTarget2 = null,
        pingTarget3 = null,
        pingCount = 4,
        runSpeedTest = true,
        thresholds = com.app.miklink.core.domain.model.TestThresholds.defaults()
    )
}

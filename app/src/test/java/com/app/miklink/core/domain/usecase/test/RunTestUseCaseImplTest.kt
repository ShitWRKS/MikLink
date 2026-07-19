package com.app.miklink.core.domain.usecase.test

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TdrCapability
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.ReportData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionPayload
import com.app.miklink.core.domain.test.model.TestSectionStatus
import com.app.miklink.core.domain.test.model.TestSkipReason
import com.app.miklink.core.domain.test.step.CableTestStep
import com.app.miklink.core.domain.test.step.LinkStatusStep
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import com.app.miklink.core.domain.test.step.PingStep
import com.app.miklink.core.domain.test.step.SpeedTestStep
import com.app.miklink.core.data.report.ReportResultsCodec
import com.app.miklink.core.domain.test.TestRunTextProvider
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

/**
 * Deterministic pure-Kotlin [TestRunTextProvider] for JVM unit tests.
 * Every method returns a stable, human-readable string so that assertions
 * against log lines remain predictable without touching Android resources.
 */
private class FakeTestRunTextProvider : TestRunTextProvider {
    override fun resultCompleted(overallStatus: String) = "Result: $overallStatus"
    override fun initStarting(clientName: String, profileName: String, socketId: String) =
        "Starting: $clientName / $profileName / $socketId"
    override fun labelInit() = "Init"
    override fun initLoading() = "Loading"
    override fun linkChecking() = "Checking link"
    override fun linkStatus(status: String, linkState: String, rate: String) =
        "Link $status: $linkState @ $rate"
    override fun linkFail(error: String) = "Link fail: $error"
    override fun linkSkip(reason: String) = "Link skip: $reason"
    override fun tdrStarting(testInterface: String) = "TDR start: $testInterface"
    override fun tdrStatus(status: String, entries: Int) = "TDR $status ($entries)"
    override fun tdrFail(statusLabel: String, error: String) = "TDR fail: $statusLabel - $error"
    override fun tdrSkip(reason: String) = "TDR skip: $reason"
    override fun linkCableDisconnected() = "Cable disconnected"
    override fun networkStarting(testInterface: String) = "Network start: $testInterface"
    override fun networkPass(mode: String, interfaceName: String) = "Network pass: $mode/$interfaceName"
    override fun networkFail(error: String) = "Network fail: $error"
    override fun networkSkip(reason: String) = "Network skip: $reason"
    override fun lldpStarting() = "LLDP start"
    override fun lldpPass(neighbors: Int) = "LLDP pass: $neighbors"
    override fun lldpInfo(message: String) = "LLDP info: $message"
    override fun lldpSkip(reason: String) = "LLDP skip: $reason"
    override fun pingStarting() = "Ping start"
    override fun pingStatus(status: String, targets: Int, warnSuffix: String) = "Ping $status ($targets)$warnSuffix"
    override fun pingFail(error: String) = "Ping fail: $error"
    override fun pingSkip(reason: String) = "Ping skip: $reason"
    override fun speedStarting() = "Speed start"
    override fun speedStatus(status: String, download: String, upload: String, warnSuffix: String) =
        "Speed $status: $download/$upload$warnSuffix"
    override fun speedFail(error: String) = "Speed fail: $error"
    override fun speedSkip(reason: String) = "Speed skip: $reason"
    override fun resultError(error: String) = "Error: $error"
}

class RunTestUseCaseImplTest {

    private val clientRepository: ClientRepository = mockk()
    private val probeRepository: ProbeRepository = mockk()
    private val profileRepository: TestProfileRepository = mockk()

    private val reportResultsCodec: ReportResultsCodec = mockk()
    private val textProvider: TestRunTextProvider = FakeTestRunTextProvider()

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
        textProvider = textProvider,
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
            tdrCapability = TdrCapability.SUPPORTED,
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
        val expectedOrder = listOf("LINK", "TDR", "NETWORK", "NEIGHBORS", "PING", "SPEED")
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
    fun `link status failure runs tdr and blocks network dependent steps`() = runTest {
        var tdrCalls = 0
        var networkCalls = 0
        var pingCalls = 0
        var speedCalls = 0
        var neighborCalls = 0

        stubRepositories()
        val useCase = buildUseCase(
            linkStatusStep = object : LinkStatusStep {
                override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<LinkStatusData> {
                    return StepResult.Failed(TestError.Unexpected("link read failed"))
                }
            },
            cableTestStep = object : CableTestStep {
                override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<CableTestSummary> {
                    tdrCalls++
                    return StepResult.Success(CableTestSummary(status = "ok", entries = emptyList()))
                }
            },
            networkConfigStep = countingNetworkStep { networkCalls++ },
            pingStep = countingPingStep { pingCalls++ },
            speedTestStep = countingSpeedStep { speedCalls++ },
            neighborDiscoveryStep = countingNeighborStep { neighborCalls++ }
        )

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed

        assertEquals(1, tdrCalls)
        assertEquals(0, networkCalls)
        assertEquals(0, pingCalls)
        assertEquals(0, speedCalls)
        assertEquals(0, neighborCalls)
        assertEquals("FAIL", completed.outcome.overallStatus)
    }

    @Test
    fun `tdr failure blocks network dependent steps`() = runTest {
        var networkCalls = 0
        var pingCalls = 0
        var speedCalls = 0
        var neighborCalls = 0

        stubRepositories()
        val useCase = buildUseCase(
            cableTestStep = object : CableTestStep {
                override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<CableTestSummary> {
                    return StepResult.Failed(TestError.Unexpected("tdr failed"))
                }
            },
            networkConfigStep = countingNetworkStep { networkCalls++ },
            pingStep = countingPingStep { pingCalls++ },
            speedTestStep = countingSpeedStep { speedCalls++ },
            neighborDiscoveryStep = countingNeighborStep { neighborCalls++ }
        )

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed

        assertEquals(0, networkCalls)
        assertEquals(0, pingCalls)
        assertEquals(0, speedCalls)
        assertEquals(0, neighborCalls)
        assertEquals("FAIL", completed.outcome.overallStatus)
    }

    @Test
    fun `tdr skipped does not block when link status passes`() = runTest {
        var networkCalls = 0

        stubRepositories()
        val useCase = buildUseCase(
            cableTestStep = object : CableTestStep {
                override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<CableTestSummary> {
                    return StepResult.Skipped(TestSkipReason.HARDWARE_UNSUPPORTED)
                }
            },
            networkConfigStep = countingNetworkStep { networkCalls++ }
        )

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed
        val sections = completed.outcome.finalSnapshot.sections.associateBy { it.id }

        assertEquals(1, networkCalls)
        assertEquals(TestSectionStatus.SKIP, sections[TestSectionId.TDR]?.status)
        assertEquals("PASS", completed.outcome.overallStatus)
    }

    @Test
    fun `layer 1 steps run before network configuration`() = runTest {
        val order = mutableListOf<String>()

        stubRepositories(profile = defaultProfile().copy(runLldp = false, runPing = false, runSpeedTest = false))
        val useCase = buildUseCase(
            linkStatusStep = object : LinkStatusStep {
                override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<LinkStatusData> {
                    order += "LINK"
                    return StepResult.Success(LinkStatusData(status = "up", rate = "1G"))
                }
            },
            cableTestStep = object : CableTestStep {
                override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<CableTestSummary> {
                    order += "TDR"
                    return StepResult.Success(CableTestSummary(status = "ok", entries = emptyList()))
                }
            },
            networkConfigStep = countingNetworkStep { order += "NETWORK" }
        )

        useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()

        assertEquals(listOf("LINK", "TDR", "NETWORK"), order)
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
            textProvider = textProvider,
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

        val plan = TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)
        useCase.execute(plan).toList()

        assertNotNull(captured.captured.speedTest)
        assertEquals("900", captured.captured.speedTest?.tcpDownload)
    }

    @Test
    fun `execute records enabled empty neighbor discovery as info without failing run`() = runTest {
        val captured = slot<ReportData>()
        val emptyNeighborStep = object : NeighborDiscoveryStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<NeighborData>> {
                return StepResult.Success(emptyList())
            }
        }

        coEvery { clientRepository.getClient(1) } returns defaultClient()
        coEvery { probeRepository.getProbeConfig() } returns defaultProbe()
        coEvery { profileRepository.getProfile(1) } returns defaultProfile()
        every { reportResultsCodec.encode(capture(captured)) } returns Result.success("{}")

        val useCase = RunTestUseCaseImpl(
            textProvider = textProvider,
            clientRepository = clientRepository,
            probeRepository = probeRepository,
            testProfileRepository = profileRepository,
            networkConfigStep = networkStep,
            linkStatusStep = linkStatusStep,
            cableTestStep = cableTestStep,
            neighborDiscoveryStep = emptyNeighborStep,
            pingStep = pingStep,
            speedTestStep = speedTestStep,
            reportResultsCodec = reportResultsCodec
        )

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed
        val neighborsSection = completed.outcome.finalSnapshot.sections.firstOrNull { it.id == TestSectionId.NEIGHBORS }

        assertNotNull(neighborsSection)
        assertEquals(TestSectionStatus.INFO, neighborsSection?.status)
        assertEquals("PASS", completed.outcome.overallStatus)
        assertTrue(captured.captured.neighbors.isEmpty())
    }

    @Test
    fun `execute records enabled neighbor discovery with neighbors as info without failing run`() = runTest {
        val discoveredNeighbor = NeighborData(
            identity = "Switch-1",
            interfaceName = "ether1",
            discoveredBy = "LLDP",
            vlanId = null,
            voiceVlanId = null,
            poeClass = null,
            systemDescription = null,
            portId = null
        )
        val neighborStep = object : NeighborDiscoveryStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<NeighborData>> {
                return StepResult.Success(listOf(discoveredNeighbor))
            }
        }

        coEvery { clientRepository.getClient(1) } returns defaultClient()
        coEvery { probeRepository.getProbeConfig() } returns defaultProbe()
        coEvery { profileRepository.getProfile(1) } returns defaultProfile()
        every { reportResultsCodec.encode(any()) } returns Result.success("{}")

        val useCase = RunTestUseCaseImpl(
            textProvider = textProvider,
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

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed
        val neighborsSection = completed.outcome.finalSnapshot.sections.firstOrNull { it.id == TestSectionId.NEIGHBORS }
        val payload = neighborsSection?.payload as? TestSectionPayload.Neighbors

        assertNotNull(neighborsSection)
        assertEquals(TestSectionStatus.INFO, neighborsSection?.status)
        assertNotNull(payload)
        assertEquals(listOf(discoveredNeighbor), payload?.entries)
        assertEquals("PASS", completed.outcome.overallStatus)
    }

    @Test
    fun `execute marks overall fail when enabled neighbor discovery fails`() = runTest {
        val failingNeighborStep = object : NeighborDiscoveryStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<NeighborData>> {
                return StepResult.Failed(TestError.Unexpected("LLDP discovery failed"))
            }
        }

        coEvery { clientRepository.getClient(1) } returns defaultClient()
        coEvery { probeRepository.getProbeConfig() } returns defaultProbe()
        coEvery { profileRepository.getProfile(1) } returns defaultProfile()
        every { reportResultsCodec.encode(any()) } returns Result.success("{}")

        val useCase = RunTestUseCaseImpl(
            textProvider = textProvider,
            clientRepository = clientRepository,
            probeRepository = probeRepository,
            testProfileRepository = profileRepository,
            networkConfigStep = networkStep,
            linkStatusStep = linkStatusStep,
            cableTestStep = cableTestStep,
            neighborDiscoveryStep = failingNeighborStep,
            pingStep = pingStep,
            speedTestStep = speedTestStep,
            reportResultsCodec = reportResultsCodec
        )

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed
        val sections = completed.outcome.finalSnapshot.sections.associateBy { it.id }

        assertEquals(TestSectionStatus.FAIL, sections[TestSectionId.NEIGHBORS]?.status)
        assertEquals("FAIL", completed.outcome.overallStatus)
    }

    @Test
    fun `execute does not run or produce neighbor section when discovery is disabled`() = runTest {
        val captured = slot<ReportData>()
        var neighborRunCalls = 0
        val disabledNeighborStep = object : NeighborDiscoveryStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<NeighborData>> {
                neighborRunCalls++
                return StepResult.Failed(TestError.Unexpected("Neighbor discovery should not run"))
            }
        }

        coEvery { clientRepository.getClient(1) } returns defaultClient()
        coEvery { probeRepository.getProbeConfig() } returns defaultProbe()
        coEvery { profileRepository.getProfile(1) } returns defaultProfile().copy(runLldp = false)
        every { reportResultsCodec.encode(capture(captured)) } returns Result.success("{}")

        val useCase = RunTestUseCaseImpl(
            textProvider = textProvider,
            clientRepository = clientRepository,
            probeRepository = probeRepository,
            testProfileRepository = profileRepository,
            networkConfigStep = networkStep,
            linkStatusStep = linkStatusStep,
            cableTestStep = cableTestStep,
            neighborDiscoveryStep = disabledNeighborStep,
            pingStep = pingStep,
            speedTestStep = speedTestStep,
            reportResultsCodec = reportResultsCodec
        )

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed
        val snapshots = events.filterIsInstance<TestEvent.SnapshotUpdated>()

        assertEquals(0, neighborRunCalls)
        assertTrue(completed.outcome.finalSnapshot.sections.none { it.id == TestSectionId.NEIGHBORS })
        assertTrue(snapshots.all { snapshot -> snapshot.snapshot.sections.none { it.id == TestSectionId.NEIGHBORS } })
        assertTrue(captured.captured.neighbors.isEmpty())
        assertTrue(captured.captured.extra.keys.none { it.equals("lldp", ignoreCase = true) })
    }

    @Test
    fun `execute marks overall fail when speed test step fails`() = runTest {
        val failingSpeedStep = object : SpeedTestStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<SpeedTestData> {
                return StepResult.Failed(
                    TestError.ConfigurationError("Speed test server is not configured")
                )
            }
        }

        coEvery { clientRepository.getClient(1) } returns defaultClient()
        coEvery { probeRepository.getProbeConfig() } returns defaultProbe()
        coEvery { profileRepository.getProfile(1) } returns defaultProfile()
        every { reportResultsCodec.encode(any()) } returns Result.success("{}")

        val useCase = RunTestUseCaseImpl(
            textProvider = textProvider,
            clientRepository = clientRepository,
            probeRepository = probeRepository,
            testProfileRepository = profileRepository,
            networkConfigStep = networkStep,
            linkStatusStep = linkStatusStep,
            cableTestStep = cableTestStep,
            neighborDiscoveryStep = neighborStep,
            pingStep = pingStep,
            speedTestStep = failingSpeedStep,
            reportResultsCodec = reportResultsCodec
        )

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed
        val sections = completed.outcome.finalSnapshot.sections.associateBy { it.id }

        assertEquals(TestSectionStatus.FAIL, sections[TestSectionId.SPEED]?.status)
        assertEquals("FAIL", completed.outcome.overallStatus)
    }

    @Test
    fun `execute does not run or produce speed section when speed test is disabled`() = runTest {
        val captured = slot<com.app.miklink.core.domain.model.report.ReportData>()
        var speedRunCalls = 0
        val disabledSpeedStep = object : SpeedTestStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<SpeedTestData> {
                speedRunCalls++
                return StepResult.Failed(TestError.Unexpected("Speed test should not run"))
            }
        }

        coEvery { clientRepository.getClient(1) } returns defaultClient()
        coEvery { probeRepository.getProbeConfig() } returns defaultProbe()
        coEvery { profileRepository.getProfile(1) } returns defaultProfile().copy(runSpeedTest = false)
        every { reportResultsCodec.encode(capture(captured)) } returns Result.success("{}")

        val useCase = RunTestUseCaseImpl(
            textProvider = textProvider,
            clientRepository = clientRepository,
            probeRepository = probeRepository,
            testProfileRepository = profileRepository,
            networkConfigStep = networkStep,
            linkStatusStep = linkStatusStep,
            cableTestStep = cableTestStep,
            neighborDiscoveryStep = neighborStep,
            pingStep = pingStep,
            speedTestStep = disabledSpeedStep,
            reportResultsCodec = reportResultsCodec
        )

        val events = useCase.execute(TestPlan(clientId = 1, profileId = 1, socketId = "A1", notes = null)).toList()
        val completed = events.last { it is TestEvent.Completed } as TestEvent.Completed
        val snapshots = events.filterIsInstance<TestEvent.SnapshotUpdated>()

        assertEquals(0, speedRunCalls)
        assertTrue(completed.outcome.finalSnapshot.sections.none { it.id == TestSectionId.SPEED })
        assertTrue(snapshots.all { snapshot -> snapshot.snapshot.sections.none { it.id == TestSectionId.SPEED } })
        assertEquals(null, captured.captured.speedTest)
        assertTrue(captured.captured.extra.keys.none { it.equals("speed", ignoreCase = true) })
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

        val cancellingPingStep = object : PingStep {
            override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<PingTargetOutcome>> {
                throw CancellationException("screen disposed")
            }
        }

        val useCase = RunTestUseCaseImpl(
            textProvider = textProvider,
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

    private fun stubRepositories(
        client: Client = defaultClient(),
        probe: ProbeConfig = defaultProbe(),
        profile: TestProfile = defaultProfile()
    ) {
        coEvery { clientRepository.getClient(1) } returns client
        coEvery { probeRepository.getProbeConfig() } returns probe
        coEvery { profileRepository.getProfile(1) } returns profile
        every { reportResultsCodec.encode(any()) } returns Result.success("{}")
    }

    private fun buildUseCase(
        networkConfigStep: NetworkConfigStep = networkStep,
        linkStatusStep: LinkStatusStep = this.linkStatusStep,
        cableTestStep: CableTestStep = this.cableTestStep,
        neighborDiscoveryStep: NeighborDiscoveryStep = neighborStep,
        pingStep: PingStep = this.pingStep,
        speedTestStep: SpeedTestStep = this.speedTestStep
    ): RunTestUseCaseImpl = RunTestUseCaseImpl(
        textProvider = textProvider,
        clientRepository = clientRepository,
        probeRepository = probeRepository,
        testProfileRepository = profileRepository,
        networkConfigStep = networkConfigStep,
        linkStatusStep = linkStatusStep,
        cableTestStep = cableTestStep,
        neighborDiscoveryStep = neighborDiscoveryStep,
        pingStep = pingStep,
        speedTestStep = speedTestStep,
        reportResultsCodec = reportResultsCodec
    )

    private fun countingNetworkStep(onRun: () -> Unit): NetworkConfigStep = object : NetworkConfigStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<NetworkConfigFeedback> {
            onRun()
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

    private fun countingNeighborStep(onRun: () -> Unit): NeighborDiscoveryStep = object : NeighborDiscoveryStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<NeighborData>> {
            onRun()
            return StepResult.Success(emptyList())
        }
    }

    private fun countingPingStep(onRun: () -> Unit): PingStep = object : PingStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<List<PingTargetOutcome>> {
            onRun()
            return StepResult.Success(emptyList())
        }
    }

    private fun countingSpeedStep(onRun: () -> Unit): SpeedTestStep = object : SpeedTestStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult<SpeedTestData> {
            onRun()
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
        tdrCapability = TdrCapability.SUPPORTED,
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

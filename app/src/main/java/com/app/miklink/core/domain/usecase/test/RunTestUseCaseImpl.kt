package com.app.miklink.core.domain.usecase.test

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NetworkData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.PingSample
import com.app.miklink.core.domain.model.report.ReportData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.model.report.TdrEntry
import com.app.miklink.core.data.report.ReportResultsCodec
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.test.logging.DebugTraceRunContext
import com.app.miklink.core.domain.test.logging.DebugTraceCorrelation
import com.app.miklink.core.domain.test.logging.DebugTraceSink
import com.app.miklink.core.domain.test.logging.LogSanitizer
import com.app.miklink.core.domain.test.logging.NoOpDebugTraceSink
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestProgress
import com.app.miklink.core.domain.test.model.TestProgressKey
import com.app.miklink.core.domain.test.model.TestRunSnapshot
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionPayload
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.core.domain.test.model.TestSectionStatus
import com.app.miklink.core.domain.test.model.TestSkipReason
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionException
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestRunTermination
import com.app.miklink.core.domain.test.step.CableTestStep
import com.app.miklink.core.domain.test.step.LinkStatusStep
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import com.app.miklink.core.domain.test.step.PingStep
import com.app.miklink.core.domain.test.step.SpeedTestStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import com.app.miklink.core.domain.policy.TestQualityPolicy
import com.app.miklink.core.domain.test.TestRunTextProvider
import com.app.miklink.utils.normalizeTime

/** RunTestUseCase implementation. Pure Kotlin: no Android/UI/data-implementation coupling (ADR-0013). */
class RunTestUseCaseImpl @Inject constructor(
    private val textProvider: TestRunTextProvider,
    private val clientRepository: ClientRepository,
    private val probeRepository: ProbeRepository,
    private val testProfileRepository: TestProfileRepository,
    private val networkConfigStep: NetworkConfigStep,
    private val linkStatusStep: LinkStatusStep,
    private val cableTestStep: CableTestStep,
    private val neighborDiscoveryStep: NeighborDiscoveryStep,
    private val pingStep: PingStep,
    private val speedTestStep: SpeedTestStep,
    private val reportResultsCodec: ReportResultsCodec,
    private val debugTraceSink: DebugTraceSink = NoOpDebugTraceSink,
    private val debugTraceRunContext: DebugTraceRunContext = DebugTraceRunContext()
) : RunTestUseCase {
    private val logSanitizer = LogSanitizer()
    private val qualityPolicy = TestQualityPolicy { testName, fields ->
        val correlation = debugTraceRunContext.correlation() ?: return@TestQualityPolicy
        debugTraceSink.correlatedEvent(
            correlation = correlation,
            event = "threshold_evaluation",
            fields = mapOf("test" to testName) + fields
        )
    }

    override fun execute(plan: TestPlan): Flow<TestEvent> = flow {
        val requestedCorrelation = debugTraceRunContext.correlation()
        val runId = debugTraceSink.startRun(
            source = "ui",
            fields = mapOf(
                "sessionId" to requestedCorrelation?.sessionId,
                "scenarioId" to requestedCorrelation?.scenarioId,
                "clientId" to plan.clientId,
                "profileId" to plan.profileId,
                "socketId" to plan.socketId
            )
        )
        var finalStatusForTrace = TraceTerminalStatus.FAIL
        var primaryFailure: Throwable? = null

        fun traceEvent(event: String, fields: Map<String, Any?> = emptyMap()) {
            val correlation = debugTraceRunContext.correlation()
            if (correlation != null) {
                debugTraceSink.correlatedEvent(correlation, event, fields)
            } else {
                debugTraceSink.event(runId = runId, event = event, fields = fields)
            }
        }

        suspend fun emitLog(message: String) {
            val sanitized = logSanitizer.sanitize(message)
            if (sanitized.isNotBlank()) {
                emit(TestEvent.LogLine(sanitized))
            }
        }

        try {
            debugTraceRunContext.set(
                DebugTraceCorrelation(
                    runId = runId,
                    sessionId = requestedCorrelation?.sessionId ?: runId,
                    scenarioId = requestedCorrelation?.scenarioId ?: "ui-run"
                )
            )

        traceEvent(
            event = "run_started",
            fields = mapOf(
                "source" to "ui",
                "clientId" to plan.clientId,
                "profileId" to plan.profileId,
                "socketId" to plan.socketId
            )
        )

        val client = clientRepository.getClient(plan.clientId)
            ?: throw IllegalStateException("Client not found: ${plan.clientId}")
        val probe = probeRepository.getProbeConfig()
            ?: throw IllegalStateException("Probe (singleton) not configured")
        val profile = testProfileRepository.getProfile(plan.profileId)
            ?: throw IllegalStateException("Profile not found: ${plan.profileId}")
        val thresholds = profile.thresholds

        traceEvent(
            event = "profile_loaded",
            fields = mapOf(
                "profileId" to profile.profileId,
                "profileName" to profile.profileName
            )
        )
        traceEvent(
            event = "test_enabled_state",
            fields = mapOf(
                "runLinkStatus" to profile.runLinkStatus,
                "runTdr" to profile.runTdr,
                "runLldp" to profile.runLldp,
                "runPing" to profile.runPing,
                "runSpeedTest" to profile.runSpeedTest
            )
        )
        traceEvent(
            event = "thresholds_loaded",
            fields = mapOf(
                "thresholds" to thresholds
            )
        )

        val testExecutionContext = TestExecutionContext(
            client = client,
            probeConfig = probe,
            testProfile = profile,
            socketId = plan.socketId,
            notes = plan.notes
        )

        val typedSections = buildInitialTypedSections(profile, probe)
        val reportData = ReportDataAccumulator()
        var overallStatus = "PASS"
        var snapshotProgressKey = TestProgressKey.PREPARING
        var snapshotPercent = 0
        var layer1Failed = false

        suspend fun emitSnapshot() {
            traceEvent(
                event = "ui_snapshot",
                fields = mapOf(
                    "progress" to snapshotProgressKey.name,
                    "percent" to snapshotPercent,
                    "sections" to typedSections.map { section ->
                        mapOf("id" to section.id.name, "status" to section.status.name)
                    }
                )
            )
            emit(
                TestEvent.SnapshotUpdated(
                    TestRunSnapshot(
                        sections = typedSectionsSnapshot(typedSections),
                        progress = snapshotProgressKey,
                        percent = snapshotPercent
                    )
                )
            )
        }

        suspend fun emitProgress(key: TestProgressKey, percent: Int, label: String, message: String) {
            snapshotProgressKey = key
            snapshotPercent = percent
            emit(TestEvent.Progress(TestProgress(label, percent, message)))
            emitSnapshot()
        }

        fun recordStep(
            id: TestSectionId,
            status: TestSectionStatus,
            title: String,
            payload: TestSectionPayload = TestSectionPayload.None,
            warning: String? = null,
            rawData: Map<String, Any?>? = null,
            error: String? = null
        ) {
            updateTypedSection(
                typedSections = typedSections,
                id = id,
                status = status,
                payload = payload,
                warning = warning,
                title = title
            )
            reportData.addExtraStep(
                name = id.toLegacyName(),
                status = status.name,
                rawData = rawData,
                error = error
            )
            traceEvent(
                event = "normalized_result",
                fields = mapOf(
                    "test" to id.toLegacyName(),
                    "title" to title,
                    "status" to status.name,
                    "warning" to warning,
                    "rawData" to rawData,
                    "payload" to payload
                )
            )
            traceEvent(
                event = "test_decision",
                fields = mapOf(
                    "test" to id.toLegacyName(),
                    "status" to status.name,
                    "reason" to (warning ?: error)
                )
            )
            if (!error.isNullOrBlank()) {
                traceEvent(
                    event = "technical_error",
                    fields = mapOf(
                        "test" to id.toLegacyName(),
                        "message" to error
                    )
                )
            }
        }

        suspend fun finishTest() {
            val json = buildReportData(plan, reportData)

            emitProgress(TestProgressKey.COMPLETED, 100, "Completato", "Test completato")

            val finalSnapshot = TestRunSnapshot(
                sections = typedSectionsSnapshot(typedSections),
                progress = snapshotProgressKey,
                percent = snapshotPercent
            )
            val outcome = TestOutcome(
                overallStatus = overallStatus,
                finalSnapshot = finalSnapshot,
                rawResultsJson = json
            )

            finalStatusForTrace = if (overallStatus == "PASS") {
                TraceTerminalStatus.SUCCESS
            } else {
                TraceTerminalStatus.FAIL
            }
            emitLog(textProvider.resultCompleted(overallStatus))
            emit(TestEvent.Completed(outcome))
        }

        /** Marks only unfinished sections as skipped, preserving completed results. */
        fun skipUnfinishedSections(
            sections: MutableList<TestSectionSnapshot>,
            reason: String
        ) {
            for (i in sections.indices) {
                val section = sections[i]
                if (section.status == TestSectionStatus.PENDING || section.status == TestSectionStatus.RUNNING) {
                    sections[i] = section.copy(
                        status = TestSectionStatus.SKIP,
                        warning = reason
                    )
                }
            }
        }

        /**
         * Handles probe disconnection during any step.
         * Marks current section as FAIL, remaining enabled sections as SKIP/PROBE_UNAVAILABLE,
         * and emits Completed with partial report.
         */
        suspend fun finishForProbeUnavailable(
            currentSection: TestSectionId,
            error: TestError.ProbeUnavailable
        ) {
            overallStatus = "FAIL"

            // Mark current section as FAIL if not already recorded
            val currentSnapshot = typedSections.firstOrNull { it.id == currentSection }
            if (currentSnapshot != null && currentSnapshot.status != TestSectionStatus.FAIL) {
                updateTypedSection(
                    typedSections = typedSections,
                    id = currentSection,
                    status = TestSectionStatus.FAIL,
                    warning = error.message
                )
            }

            // Mark remaining PENDING/RUNNING sections as SKIP with PROBE_UNAVAILABLE
            skipUnfinishedSections(typedSections, TestSkipReason.PROBE_UNAVAILABLE)

            // Build final outcome
            val finalSnapshot = TestRunSnapshot(
                sections = typedSectionsSnapshot(typedSections),
                progress = snapshotProgressKey,
                percent = snapshotPercent
            )

            // Add termination info to report
            reportData.addExtraStep(
                name = "termination",
                status = "PROBE_UNAVAILABLE",
                rawData = mapOf(
                    "termination" to "PROBE_UNAVAILABLE",
                    "terminalErrorType" to "ProbeUnavailable",
                    "terminalErrorMessage" to (error.message.take(200))
                ),
                error = null
            )

            val json = buildReportData(plan, reportData)

            val outcome = TestOutcome(
                overallStatus = overallStatus,
                finalSnapshot = finalSnapshot,
                rawResultsJson = json,
                termination = TestRunTermination.PROBE_UNAVAILABLE,
                terminalError = error
            )

            finalStatusForTrace = TraceTerminalStatus.FAIL
            emitLog(textProvider.resultCompleted(overallStatus))
            emit(TestEvent.Completed(outcome))
        }

        emitSnapshot()
        emitLog(textProvider.initStarting(client.companyName, profile.profileName, plan.socketId ?: ""))
        emitProgress(TestProgressKey.PREPARING, 0, textProvider.labelInit(), textProvider.initLoading())

            // 1) Link Status
            if (profile.runLinkStatus) {
                // Cooperative cancellation checkpoint: allows coroutine to be cancelled before long step
                coroutineContext.ensureActive()
                emitProgress(TestProgressKey.LINK, 10, "Link Status", "Verifica stato link...")

                updateTypedSection(
                    typedSections = typedSections,
                    id = TestSectionId.LINK,
                    status = TestSectionStatus.RUNNING,
                    title = "Link"
                )
                emitSnapshot()
                emitLog(textProvider.linkChecking())

                when (val linkResult = linkStatusStep.run(testExecutionContext)) {
                    is StepResult.Success -> {
                        val linkStatus = linkResult.data
                        reportData.linkStatus = linkStatus
                        val evaluation = qualityPolicy.evaluateLink(linkStatus, profile, client)
                        val cableDisconnected = isCableDisconnected(linkStatus.status)
                        val resolvedStatus = if (cableDisconnected) TestSectionStatus.FAIL else evaluation.status
                        val resolvedWarning = if (cableDisconnected) {
                            evaluation.warning ?: "Link inattivo o sconosciuto"
                        } else {
                            evaluation.warning
                        }
                        if (resolvedStatus == TestSectionStatus.FAIL) {
                            overallStatus = "FAIL"
                            layer1Failed = true
                        }
                        recordStep(
                            id = TestSectionId.LINK,
                            title = "Link",
                            status = resolvedStatus,
                            rawData = linkRaw(linkStatus),
                            payload = TestSectionPayload.Link(linkStatus),
                            warning = resolvedWarning
                        )
                        emitSnapshot()
                        emitLog(textProvider.linkStatus(resolvedStatus.name, linkStatus.status ?: "-", linkStatus.rate ?: "-"))
                    }
                    is StepResult.Failed -> {
                        if (linkResult.error is TestError.ProbeUnavailable) {
                            finishForProbeUnavailable(TestSectionId.LINK, linkResult.error as TestError.ProbeUnavailable)
                            return@flow
                        }
                        overallStatus = "FAIL"
                        layer1Failed = true
                        val errorMessage = linkResult.error.message
                        recordStep(
                            id = TestSectionId.LINK,
                            title = "Link",
                            status = TestSectionStatus.FAIL,
                            warning = errorMessage,
                            rawData = mapOf("error" to errorMessage),
                            error = errorMessage
                        )
                        emitSnapshot()
                        emitLog(textProvider.linkFail(errorMessage ?: "unknown error"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            id = TestSectionId.LINK,
                            title = "Link",
                            status = TestSectionStatus.SKIP,
                            warning = linkResult.reason,
                            rawData = mapOf("reason" to linkResult.reason)
                        )
                        emitSnapshot()
                        emitLog(textProvider.linkSkip(linkResult.reason))
                    }
                }
            } else {
                emitLog(textProvider.linkSkip(TestSkipReason.PROFILE_DISABLED))
            }

            // 2) TDR
            if (profile.runTdr && probe.shouldAttemptTdr) {
                // Cooperative cancellation checkpoint
                coroutineContext.ensureActive()
                emitProgress(TestProgressKey.TDR, 30, "TDR", "Test cavo in corso...")

                updateTypedSection(
                    typedSections = typedSections,
                    id = TestSectionId.TDR,
                    status = TestSectionStatus.RUNNING,
                    title = "TDR"
                )
                emitSnapshot()
                emitLog(textProvider.tdrStarting(probe.testInterface))

                when (val tdrResult = cableTestStep.run(testExecutionContext)) {
                    is StepResult.Success -> {
                        val cableTest = tdrResult.data
                        reportData.tdr += cableTest.entries
                        val evaluation = qualityPolicy.evaluateTdr(cableTest, profile)
                        if (evaluation.status == TestSectionStatus.FAIL) {
                            overallStatus = "FAIL"
                            layer1Failed = true
                        }
                        recordStep(
                            id = TestSectionId.TDR,
                            title = "TDR",
                            status = evaluation.status,
                            rawData = tdrRaw(cableTest),
                            payload = TestSectionPayload.Tdr(cableTest.entries),
                            warning = evaluation.warning
                        )
                        emitSnapshot()
                        emitLog(textProvider.tdrStatus(evaluation.status.name, cableTest.entries.size))
                    }
                    is StepResult.Failed -> {
                        if (tdrResult.error is TestError.ProbeUnavailable) {
                            finishForProbeUnavailable(TestSectionId.TDR, tdrResult.error as TestError.ProbeUnavailable)
                            return@flow
                        }
                        val unsupportedOnUnknown =
                            probe.tdrCapability == com.app.miklink.core.domain.model.TdrCapability.UNKNOWN &&
                                tdrResult.error is TestError.Unsupported
                        if (!unsupportedOnUnknown) {
                            overallStatus = "FAIL"
                            layer1Failed = true
                        }
                        val status = if (unsupportedOnUnknown) TestSectionStatus.SKIP else TestSectionStatus.FAIL
                        val message = tdrResult.error.message
                        recordStep(
                            id = TestSectionId.TDR,
                            title = "TDR",
                            status = status,
                            warning = if (unsupportedOnUnknown) TestSkipReason.HARDWARE_UNSUPPORTED else message,
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emitSnapshot()
                        val statusLabel = if (unsupportedOnUnknown) "SKIP" else "FAIL"
                        emitLog(textProvider.tdrFail(statusLabel, message ?: "unknown error"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            id = TestSectionId.TDR,
                            title = "TDR",
                            status = TestSectionStatus.SKIP,
                            warning = tdrResult.reason,
                            rawData = mapOf("reason" to tdrResult.reason)
                        )
                        emitSnapshot()
                        emitLog(textProvider.tdrSkip(tdrResult.reason))
                    }
                }
            } else if (profile.runTdr && !probe.shouldAttemptTdr) {
                recordStep(
                    id = TestSectionId.TDR,
                    title = "TDR",
                    status = TestSectionStatus.SKIP,
                    warning = TestSkipReason.HARDWARE_UNSUPPORTED,
                    rawData = mapOf("reason" to TestSkipReason.HARDWARE_UNSUPPORTED)
                )
                emitSnapshot()
                emitLog(textProvider.tdrSkip(TestSkipReason.HARDWARE_UNSUPPORTED))
            } else {
                emitLog(textProvider.tdrSkip(TestSkipReason.PROFILE_DISABLED))
            }

            if (layer1Failed) {
                skipUnfinishedSections(typedSections, TestSkipReason.LAYER1_FAILED)
                emitSnapshot()
                emitLog(textProvider.layer1FailedSkipping())
                finishTest()
                return@flow
            }

            // 3) Network Config
            // Cooperative cancellation checkpoint
            coroutineContext.ensureActive()
            emitProgress(TestProgressKey.NETWORK_CONFIG, 50, "Network Config", "Configurazione rete in corso...")

            updateTypedSection(
                typedSections = typedSections,
                id = TestSectionId.NETWORK,
                status = TestSectionStatus.RUNNING,
                title = "Network"
            )
            emitSnapshot()
            emitLog(textProvider.networkStarting(probe.testInterface))

            when (val networkResult = networkConfigStep.run(testExecutionContext)) {
                is StepResult.Success -> {
                    val feedback = networkResult.data as NetworkConfigFeedback
                    reportData.network = NetworkData(
                        mode = feedback.mode,
                        address = feedback.address,
                        gateway = feedback.gateway,
                        dns = feedback.dns,
                        message = feedback.message
                    )
                    recordStep(
                        id = TestSectionId.NETWORK,
                        status = TestSectionStatus.PASS,
                        title = "Network",
                        rawData = networkRaw(feedback),
                        payload = TestSectionPayload.Network(
                            mode = feedback.mode,
                            address = feedback.address,
                            gateway = feedback.gateway,
                            dns = feedback.dns,
                            message = feedback.message
                        )
                    )
                    emitSnapshot()
                    emitLog(textProvider.networkPass(feedback.mode, feedback.interfaceName))
                }
                is StepResult.Failed -> {
                    if (networkResult.error is TestError.ProbeUnavailable) {
                        finishForProbeUnavailable(TestSectionId.NETWORK, networkResult.error as TestError.ProbeUnavailable)
                        return@flow
                    }
                    overallStatus = "FAIL"
                    val errorMessage = networkResult.error.message
                    recordStep(
                        id = TestSectionId.NETWORK,
                        title = "Network",
                        status = TestSectionStatus.FAIL,
                        warning = errorMessage,
                        rawData = mapOf("error" to errorMessage),
                        error = errorMessage
                    )
                    emitSnapshot()
                    emitLog(textProvider.networkFail(errorMessage ?: "unknown error"))
                }
                is StepResult.Skipped -> {
                    recordStep(
                        id = TestSectionId.NETWORK,
                        title = "Network",
                        status = TestSectionStatus.SKIP,
                        warning = networkResult.reason,
                        rawData = mapOf("reason" to networkResult.reason)
                    )
                    emitSnapshot()
                    emitLog(textProvider.networkSkip(networkResult.reason))
                }
            }

            // 4) LLDP
            if (profile.runLldp) {
                // Cooperative cancellation checkpoint
                coroutineContext.ensureActive()
                emitProgress(TestProgressKey.NEIGHBORS, 60, "LLDP", "Discovery neighbor...")

                updateTypedSection(
                    typedSections = typedSections,
                    id = TestSectionId.NEIGHBORS,
                    status = TestSectionStatus.RUNNING,
                    title = "LLDP/CDP"
                )
                emitSnapshot()
                emitLog(textProvider.lldpStarting())

                when (val lldpResult = neighborDiscoveryStep.run(testExecutionContext)) {
                    is StepResult.Success -> {
                        val neighbors = lldpResult.data
                        reportData.neighbors += neighbors
                        recordStep(
                            id = TestSectionId.NEIGHBORS,
                            title = "LLDP/CDP",
                            status = TestSectionStatus.INFO,
                            rawData = lldpRaw(neighbors),
                            payload = TestSectionPayload.Neighbors(neighbors)
                        )
                        emitSnapshot()
                        emitLog(textProvider.lldpPass(neighbors.size))
                    }
                    is StepResult.Failed -> {
                        if (lldpResult.error is TestError.ProbeUnavailable) {
                            finishForProbeUnavailable(TestSectionId.NEIGHBORS, lldpResult.error as TestError.ProbeUnavailable)
                            return@flow
                        }
                        overallStatus = "FAIL"
                        val message = lldpResult.error.message
                        recordStep(
                            id = TestSectionId.NEIGHBORS,
                            title = "LLDP/CDP",
                            status = TestSectionStatus.FAIL,
                            warning = message ?: "Unknown error",
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emitSnapshot()
                        emitLog(textProvider.lldpInfo(message ?: "unknown error"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            id = TestSectionId.NEIGHBORS,
                            title = "LLDP/CDP",
                            status = TestSectionStatus.SKIP,
                            warning = lldpResult.reason,
                            rawData = mapOf("reason" to lldpResult.reason)
                        )
                        emitSnapshot()
                        emitLog(textProvider.lldpSkip(lldpResult.reason))
                    }
                }
            } else {
                emitLog(textProvider.lldpSkip(TestSkipReason.PROFILE_DISABLED))
            }

            // 5) Ping
            if (profile.runPing) {
                // Cooperative cancellation checkpoint
                coroutineContext.ensureActive()
                emitProgress(TestProgressKey.PING, 70, "Ping", "Test ping in corso...")

                updateTypedSection(
                    typedSections = typedSections,
                    id = TestSectionId.PING,
                    status = TestSectionStatus.RUNNING,
                    title = "Ping"
                )
                emitSnapshot()
                emitLog(textProvider.pingStarting())

                when (val pingResult = pingStep.run(testExecutionContext)) {
                    is StepResult.Success -> {
                        val outcomes = pingResult.data
                        val samples = mapPingOutcomes(outcomes)
                        reportData.pingSamples += samples
                        val evaluation = qualityPolicy.evaluatePing(outcomes, profile)
                        if (evaluation.status == TestSectionStatus.FAIL) {
                            overallStatus = "FAIL"
                        }
                        recordStep(
                            id = TestSectionId.PING,
                            title = "Ping",
                            status = evaluation.status,
                            rawData = pingRaw(outcomes),
                            payload = TestSectionPayload.Ping(samples),
                            warning = evaluation.warning
                        )
                        emitSnapshot()
                        emitLog(textProvider.pingStatus(evaluation.status.name, outcomes.size, evaluation.warning?.let { " warn=$it" } ?: ""))
                    }
                    is StepResult.Failed -> {
                        if (pingResult.error is TestError.ProbeUnavailable) {
                            finishForProbeUnavailable(TestSectionId.PING, pingResult.error as TestError.ProbeUnavailable)
                            return@flow
                        }
                        overallStatus = "FAIL"
                        val error = pingResult.error.message
                        recordStep(
                            id = TestSectionId.PING,
                            title = "Ping",
                            status = TestSectionStatus.FAIL,
                            warning = error,
                            rawData = mapOf("error" to error),
                            error = error
                        )
                        emitSnapshot()
                        emitLog(textProvider.pingFail(error ?: "unknown error"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            id = TestSectionId.PING,
                            title = "Ping",
                            status = TestSectionStatus.SKIP,
                            warning = pingResult.reason,
                            rawData = mapOf("reason" to pingResult.reason)
                        )
                        emitSnapshot()
                        emitLog(textProvider.pingSkip(pingResult.reason))
                    }
                }
            } else {
                recordStep(
                    id = TestSectionId.PING,
                    title = "Ping",
                    status = TestSectionStatus.SKIP,
                    warning = TestSkipReason.PROFILE_DISABLED,
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSnapshot()
                emitLog(textProvider.pingSkip(TestSkipReason.PROFILE_DISABLED))
            }

            // 6) Speed Test
            if (profile.runSpeedTest) {
                // Cooperative cancellation checkpoint
                coroutineContext.ensureActive()
                emitProgress(TestProgressKey.SPEED, 90, "Speed Test", "Speed test in corso...")

                updateTypedSection(
                    typedSections = typedSections,
                    id = TestSectionId.SPEED,
                    status = TestSectionStatus.RUNNING,
                    title = "Speed Test"
                )
                emitSnapshot()
                emitLog(textProvider.speedStarting())

                when (val speedResult = speedTestStep.run(testExecutionContext)) {
                    is StepResult.Success -> {
                        val speed = speedResult.data
                        reportData.speedTest = speed
                        val evaluation = qualityPolicy.evaluateSpeed(speed, profile)
                        if (evaluation.status == TestSectionStatus.FAIL) {
                            overallStatus = "FAIL"
                        }
                        recordStep(
                            id = TestSectionId.SPEED,
                            title = "Speed Test",
                            status = evaluation.status,
                            rawData = speedRaw(speed, client.speedTestServerAddress),
                            payload = TestSectionPayload.Speed(speed),
                            warning = evaluation.warning
                        )
                        emitSnapshot()
                        emitLog(textProvider.speedStatus(evaluation.status.name, speed.tcpDownload ?: "-", speed.tcpUpload ?: "-", evaluation.warning?.let { " warn=$it" } ?: ""))
                    }
                    is StepResult.Failed -> {
                        if (speedResult.error is TestError.ProbeUnavailable) {
                            finishForProbeUnavailable(TestSectionId.SPEED, speedResult.error as TestError.ProbeUnavailable)
                            return@flow
                        }
                        overallStatus = "FAIL"
                        val message = speedResult.error.message
                        recordStep(
                            id = TestSectionId.SPEED,
                            title = "Speed Test",
                            status = TestSectionStatus.FAIL,
                            warning = message,
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emitSnapshot()
                        emitLog(textProvider.speedFail(message ?: "unknown error"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            id = TestSectionId.SPEED,
                            title = "Speed Test",
                            status = TestSectionStatus.SKIP,
                            warning = speedResult.reason,
                            rawData = mapOf("reason" to speedResult.reason)
                        )
                        emitSnapshot()
                        emitLog(textProvider.speedSkip(speedResult.reason))
                    }
                }
            } else {
                emitLog(textProvider.speedSkip(TestSkipReason.PROFILE_DISABLED))
            }

            finishTest()
        } catch (e: CancellationException) {
            finalStatusForTrace = TraceTerminalStatus.CANCELLED
            primaryFailure = e
            throw e
        } catch (e: Exception) {
            finalStatusForTrace = TraceTerminalStatus.FAIL
            primaryFailure = e
            val error = when (e) {
                is TestExecutionException -> e.error
                else -> TestError.Unexpected(e.message ?: "Unknown error", e)
            }
            traceEvent(
                event = "technical_error",
                fields = mapOf(
                    "test" to "RUN",
                    "message" to (e.message ?: "Unknown error"),
                    "type" to e::class.java.simpleName
                )
            )
            emitLog(textProvider.resultError(e.message ?: "unknown error"))
            emit(TestEvent.Failed(error))
        } finally {
            try {
                debugTraceSink.finishRun(
                    runId = runId,
                    finalStatus = finalStatusForTrace.name
                )
            } catch (finishFailure: Throwable) {
                val failure = primaryFailure
                if (failure != null) {
                    failure.addSuppressed(finishFailure)
                } else {
                    throw finishFailure
                }
            } finally {
                debugTraceRunContext.clear(runId)
            }
        }
    }

    private fun isCableDisconnected(status: String?): Boolean {
        val normalized = status?.trim()?.lowercase()
        return normalized.isNullOrBlank() ||
            normalized == "down" ||
            normalized == "no-link" ||
            normalized == "unknown"
    }

    private fun buildReportData(plan: TestPlan, accumulator: ReportDataAccumulator): String {
        val reportData = accumulator.toReportData(plan)

        val json = try {
            reportResultsCodec.encode(reportData)
                .getOrElse { cause ->
                    throw TestExecutionException(
                        TestError.SerializationError(
                            message = cause.message ?: "Report serialization failed",
                            cause = cause
                        )
                    )
                }
        } catch (e: TestExecutionException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw TestExecutionException(
                TestError.SerializationError(
                    message = e.message ?: "Report serialization failed",
                    cause = e
                )
            )
        }

        if (json.isBlank()) {
            throw TestExecutionException(
                TestError.SerializationError(
                    message = "Report serialization produced an empty payload"
                )
            )
        }

        return json
    }

    // Flattens per-target ping results into report rows, keeping target-level loss when present.
    private fun mapPingOutcomes(outcomes: List<PingTargetOutcome>): List<PingSample> {
        val samples = mutableListOf<PingSample>()
        outcomes.forEach { outcome ->
            outcome.results.forEach { result ->
                samples += PingSample(
                    target = outcome.target,
                    host = result.host,
                    minRtt = normalizeTime(result.minRtt),
                    avgRtt = normalizeTime(result.avgRtt),
                    maxRtt = normalizeTime(result.maxRtt),
                    packetLoss = outcome.packetLoss ?: result.packetLoss,
                    sent = result.sent,
                    received = result.received,
                    seq = result.seq,
                    time = normalizeTime(result.time),
                    ttl = result.ttl,
                    size = result.size,
                    error = outcome.error
                )
            }
        }
        return samples
    }

    // Collects report payloads and per-step metadata during execution.
    private class ReportDataAccumulator {
        var network: NetworkData? = null
        var linkStatus: LinkStatusData? = null
        val tdr: MutableList<TdrEntry> = mutableListOf()
        val neighbors: MutableList<NeighborData> = mutableListOf()
        val pingSamples: MutableList<PingSample> = mutableListOf()
        var speedTest: SpeedTestData? = null
        private val extra: MutableMap<String, String> = mutableMapOf()

        fun addExtraStep(name: String, status: String, rawData: Map<String, Any?>?, error: String?) {
            val parts = mutableListOf<String>()
            parts += "status=$status"
            val sanitizedData = sanitizeRawData(rawData)
            if (sanitizedData.isNotEmpty()) parts += "data=${sanitizedData}"
            if (!error.isNullOrBlank()) parts += "error=$error"
            extra[name.lowercase()] = parts.joinToString("; ")
        }

        fun toReportData(plan: TestPlan): ReportData {
            val mergedExtra = extra.toMutableMap()
            mergedExtra.putIfAbsent(
                "plan",
                "clientId=${plan.clientId}; profileId=${plan.profileId}; socketId=${plan.socketId}"
            )
            mergedExtra.putIfAbsent("timestamp", System.currentTimeMillis().toString())
            return ReportData(
                network = network,
                linkStatus = linkStatus,
                tdr = tdr.toList(),
                neighbors = neighbors.toList(),
                pingSamples = pingSamples.toList(),
                speedTest = speedTest,
                extra = mergedExtra
            )
        }

        private fun sanitizeRawData(rawData: Map<String, Any?>?): Map<String, String> {
            rawData ?: return emptyMap()
            val sanitized = mutableMapOf<String, String>()
            rawData.forEach { (key, value) ->
                val v = sanitizeValue(value)
                if (v != null) sanitized[key] = v
            }
            return sanitized
        }

        // Normalizes mixed raw values into strings for report extras.
        private fun sanitizeValue(value: Any?): String? = when (value) {
            null -> null
            is String, is Number, is Boolean -> value.toString()
            is Map<*, *> -> value.entries.mapNotNull { (k, v) ->
                val name = k?.toString() ?: return@mapNotNull null
                "$name=${sanitizeValue(v)}"
            }.joinToString(",")
            is List<*> -> value.mapNotNull { sanitizeValue(it) }.joinToString(",")
            is NeighborData -> serializeNeighbor(value).toString()
            is PingTargetOutcome -> serializePingOutcome(value).toString()
            is PingMeasurement -> serializePingMeasurement(value).toString()
            is SpeedTestData -> serializeSpeedResult(value).toString()
            is TdrEntry -> serializeTdrEntry(value).toString()
            else -> value.toString()
        }

        private fun serializeNeighbor(neighbor: NeighborData): Map<String, Any?> {
            return mapOf(
                "identity" to neighbor.identity,
                "interface-name" to neighbor.interfaceName,
                "discovered-by" to neighbor.discoveredBy,
                "vlan-id" to neighbor.vlanId,
                "voice-vlan-id" to neighbor.voiceVlanId,
                "poe-class" to neighbor.poeClass,
                "system-description" to neighbor.systemDescription,
                "port-id" to neighbor.portId
            )
        }

        private fun serializePingOutcome(outcome: PingTargetOutcome): Map<String, Any?> {
            return mapOf(
                "target" to outcome.target,
                "packetLoss" to outcome.packetLoss,
                "error" to outcome.error,
                "results" to outcome.results.map { serializePingMeasurement(it) }
            )
        }

        private fun serializePingMeasurement(result: PingMeasurement): Map<String, Any?> {
            return mapOf(
                "host" to result.host,
                "min-rtt" to result.minRtt,
                "avg-rtt" to result.avgRtt,
                "max-rtt" to result.maxRtt,
                "packet-loss" to result.packetLoss,
                "sent" to result.sent,
                "received" to result.received,
                "seq" to result.seq,
                "time" to result.time,
                "ttl" to result.ttl,
                "size" to result.size
            )
        }

        private fun serializeSpeedResult(speed: SpeedTestData): Map<String, Any?> {
            return mapOf(
                "status" to speed.status,
                "ping" to speed.ping,
                "jitter" to speed.jitter,
                "loss" to speed.loss,
                "tcp-download" to speed.tcpDownload,
                "tcp-upload" to speed.tcpUpload,
                "udp-download" to speed.udpDownload,
                "udp-upload" to speed.udpUpload,
                "warning" to speed.warning,
                "server" to speed.serverAddress
            )
        }

        private fun serializeTdrEntry(entry: TdrEntry): Map<String, Any?> {
            return mapOf(
                "distance" to entry.distance,
                "status" to entry.status,
                "description" to entry.description
            )
        }
    }

    private fun networkRaw(feedback: NetworkConfigFeedback): Map<String, Any?> =
        linkedMapOf(
            "mode" to feedback.mode,
            "interface" to feedback.interfaceName,
            "address" to feedback.address,
            "gateway" to feedback.gateway,
            "dns" to feedback.dns,
            "message" to feedback.message
        )

    private fun linkRaw(response: LinkStatusData): Map<String, Any?> =
        linkedMapOf(
            "status" to response.status,
            "rate" to response.rate
        )

    private fun tdrRaw(summary: CableTestSummary): Map<String, Any?> =
        linkedMapOf(
            "status" to summary.status,
            "entries" to summary.entries.map { entry ->
                mapOf(
                    "distance" to entry.distance,
                    "status" to entry.status,
                    "description" to entry.description
                )
            }
        )

    private fun lldpRaw(neighbors: List<NeighborData>): Map<String, Any?> =
        linkedMapOf(
            "neighbors" to neighbors.map { neighbor ->
                mapOf(
                    "identity" to neighbor.identity,
                    "interface-name" to neighbor.interfaceName,
                    "discovered-by" to neighbor.discoveredBy,
                    "vlan-id" to neighbor.vlanId,
                    "voice-vlan-id" to neighbor.voiceVlanId,
                    "poe-class" to neighbor.poeClass,
                    "system-description" to neighbor.systemDescription,
                    "port-id" to neighbor.portId
                )
            }
        )

    private fun pingRaw(results: List<PingTargetOutcome>): Map<String, Any?> =
        linkedMapOf(
            "targets" to mapPingOutcomes(results).map { sample ->
                mapOf(
                    "target" to sample.target,
                    "host" to sample.host,
                    "min-rtt" to sample.minRtt,
                    "avg-rtt" to sample.avgRtt,
                    "max-rtt" to sample.maxRtt,
                    "packet-loss" to sample.packetLoss,
                    "sent" to sample.sent,
                    "received" to sample.received,
                    "seq" to sample.seq,
                    "time" to sample.time,
                    "ttl" to sample.ttl,
                    "size" to sample.size,
                    "error" to sample.error
                )
            }
        )

    private fun speedRaw(speed: SpeedTestData, serverAddress: String?): Map<String, Any?> =
        linkedMapOf(
            "server" to (speed.serverAddress ?: serverAddress),
            "status" to speed.status,
            "ping" to speed.ping,
            "jitter" to speed.jitter,
            "loss" to speed.loss,
            "tcpDownload" to speed.tcpDownload,
            "tcpUpload" to speed.tcpUpload,
            "udpDownload" to speed.udpDownload,
            "udpUpload" to speed.udpUpload,
            "warning" to speed.warning
        )

}

private enum class TraceTerminalStatus {
    SUCCESS,
    FAIL,
    CANCELLED
}

private const val SECTION_NETWORK = "NETWORK"
private const val SECTION_LINK = "LINK"
private const val SECTION_TDR = "TDR"
private const val SECTION_LLDP = "LLDP"
private const val SECTION_PING = "PING"
private const val SECTION_SPEED = "SPEED"

private fun TestSectionId.toLegacyName(): String = when (this) {
    TestSectionId.NETWORK -> SECTION_NETWORK
    TestSectionId.LINK -> SECTION_LINK
    TestSectionId.TDR -> SECTION_TDR
    TestSectionId.NEIGHBORS -> SECTION_LLDP
    TestSectionId.PING -> SECTION_PING
    TestSectionId.SPEED -> SECTION_SPEED
    else -> name
}

// Initializes section list with default status based on profile flags and hardware support.
private fun buildInitialTypedSections(profile: TestProfile, probe: ProbeConfig): MutableList<TestSectionSnapshot> {
    val sections = mutableListOf<TestSectionSnapshot>()
    if (profile.runLinkStatus) {
        sections += TestSectionSnapshot(id = TestSectionId.LINK, status = TestSectionStatus.PENDING, title = "Link")
    }
    if (profile.runTdr && probe.shouldAttemptTdr) {
        sections += TestSectionSnapshot(id = TestSectionId.TDR, status = TestSectionStatus.PENDING, title = "TDR")
    } else if (profile.runTdr && !probe.shouldAttemptTdr) {
        sections += TestSectionSnapshot(
            id = TestSectionId.TDR,
            status = TestSectionStatus.SKIP,
            title = "TDR",
            warning = TestSkipReason.HARDWARE_UNSUPPORTED
        )
    }
    sections += TestSectionSnapshot(
        id = TestSectionId.NETWORK,
        status = TestSectionStatus.PENDING,
        title = "Network"
    )
    if (profile.runLldp) {
        sections += TestSectionSnapshot(id = TestSectionId.NEIGHBORS, status = TestSectionStatus.PENDING, title = "LLDP/CDP")
    }
    sections += when {
        profile.runPing -> TestSectionSnapshot(id = TestSectionId.PING, status = TestSectionStatus.PENDING, title = "Ping")
        else -> TestSectionSnapshot(
            id = TestSectionId.PING,
            status = TestSectionStatus.SKIP,
            title = "Ping",
            warning = TestSkipReason.PROFILE_DISABLED
        )
    }
    if (profile.runSpeedTest) {
        sections += TestSectionSnapshot(id = TestSectionId.SPEED, status = TestSectionStatus.PENDING, title = "Speed Test")
    }
    return sections
}

private fun updateTypedSection(
    typedSections: MutableList<TestSectionSnapshot>,
    id: TestSectionId,
    status: TestSectionStatus,
    payload: TestSectionPayload = TestSectionPayload.None,
    warning: String? = null,
    title: String? = null
) {
    // Preserve existing payload unless a real payload is provided.
    val index = typedSections.indexOfFirst { it.id == id }
    val existing = typedSections.getOrNull(index)
    val resolvedPayload = if (payload is TestSectionPayload.None && existing?.payload !is TestSectionPayload.None) {
        existing?.payload ?: payload
    } else {
        payload
    }
    val newSnapshot = TestSectionSnapshot(
        id = id,
        status = status,
        payload = resolvedPayload,
        title = title ?: existing?.title,
        warning = warning ?: existing?.warning
    )
    if (index >= 0) {
        typedSections[index] = newSnapshot
    } else {
        typedSections.add(newSnapshot)
    }
}

private fun typedSectionsSnapshot(source: List<TestSectionSnapshot>): List<TestSectionSnapshot> =
    source.map { it.copy(payload = it.payload) }


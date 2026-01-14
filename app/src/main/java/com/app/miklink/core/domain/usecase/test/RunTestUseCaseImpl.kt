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
import com.app.miklink.core.domain.test.logging.LogSanitizer
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
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.step.CableTestStep
import com.app.miklink.core.domain.test.step.LinkStatusStep
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import com.app.miklink.core.domain.test.step.PingStep
import com.app.miklink.core.domain.test.step.SpeedTestStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import com.app.miklink.core.domain.policy.TestQualityPolicy
import com.app.miklink.utils.normalizeTime

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.app.miklink.R

/** RunTestUseCase implementation. */
class RunTestUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clientRepository: ClientRepository,
    private val probeRepository: ProbeRepository,
    private val testProfileRepository: TestProfileRepository,
    private val networkConfigStep: NetworkConfigStep,
    private val linkStatusStep: LinkStatusStep,
    private val cableTestStep: CableTestStep,
    private val neighborDiscoveryStep: NeighborDiscoveryStep,
    private val pingStep: PingStep,
    private val speedTestStep: SpeedTestStep,
    private val reportResultsCodec: ReportResultsCodec
) : RunTestUseCase {
    private val logSanitizer = LogSanitizer()
    private val qualityPolicy = TestQualityPolicy()

    override fun execute(plan: TestPlan): Flow<TestEvent> = flow {
        suspend fun emitLog(message: String) {
            val sanitized = logSanitizer.sanitize(message)
            if (sanitized.isNotBlank()) {
                emit(TestEvent.LogLine(sanitized))
            }
        }

        val client = clientRepository.getClient(plan.clientId)
            ?: throw IllegalStateException("Client not found: ${plan.clientId}")
        val probe = probeRepository.getProbeConfig()
            ?: throw IllegalStateException("Probe (singleton) not configured")
        val profile = testProfileRepository.getProfile(plan.profileId)
            ?: throw IllegalStateException("Profile not found: ${plan.profileId}")

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
        var stopAfterLink = false
        var stopAfterLinkReason: String? = null

        suspend fun emitSnapshot() {
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
        }

        suspend fun finishTest() {
            emitProgress(TestProgressKey.COMPLETED, 100, "Completato", "Test completato")

            val finalSnapshot = TestRunSnapshot(
                sections = typedSectionsSnapshot(typedSections),
                progress = snapshotProgressKey,
                percent = snapshotPercent
            )
            val outcome = TestOutcome(
                overallStatus = overallStatus,
                finalSnapshot = finalSnapshot,
                rawResultsJson = buildReportData(plan, reportData)
            )

            emitLog(context.getString(R.string.log_result_completed, overallStatus))
            emit(TestEvent.Completed(outcome))
        }

        suspend fun skipRemainingSections(reason: String) {
            val remaining = listOf(
                TestSectionId.NETWORK,
                TestSectionId.TDR,
                TestSectionId.NEIGHBORS,
                TestSectionId.PING,
                TestSectionId.SPEED
            )
            remaining.forEach { id ->
                val existingStatus = typedSections.firstOrNull { it.id == id }?.status
                if (existingStatus == TestSectionStatus.SKIP) return@forEach
                val title = when (id) {
                    TestSectionId.NETWORK -> "Network"
                    TestSectionId.TDR -> "TDR"
                    TestSectionId.NEIGHBORS -> "LLDP/CDP"
                    TestSectionId.PING -> "Ping"
                    TestSectionId.SPEED -> "Speed Test"
                    else -> id.name
                }
                recordStep(
                    id = id,
                    title = title,
                    status = TestSectionStatus.SKIP,
                    warning = reason,
                    rawData = mapOf("reason" to reason)
                )
            }
            emitSnapshot()
        }

        emitSnapshot()
        emitLog(context.getString(R.string.log_init_starting, client.companyName, profile.profileName, plan.socketId))
        emitProgress(TestProgressKey.PREPARING, 0, context.getString(R.string.log_label_init), context.getString(R.string.log_init_loading))

        try {
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
                emitLog(context.getString(R.string.log_link_checking))

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
                        emitLog(context.getString(R.string.log_link_status, resolvedStatus, linkStatus.status ?: "-", linkStatus.rate ?: "-"))
                        if (cableDisconnected) {
                            stopAfterLink = true
                            stopAfterLinkReason = resolvedWarning
                        }
                    }
                    is StepResult.Failed -> {
                        overallStatus = "FAIL"
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
                        emitLog(context.getString(R.string.log_link_fail, errorMessage ?: "unknown error"))
                        emit(TestEvent.Failed(linkResult.error))
                        return@flow
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
                        emitLog(context.getString(R.string.log_link_skip, linkResult.reason))
                    }
                }
            } else {
                recordStep(
                    id = TestSectionId.LINK,
                    title = "Link",
                    status = TestSectionStatus.SKIP,
                    warning = TestSkipReason.PROFILE_DISABLED,
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSnapshot()
                emitLog(context.getString(R.string.log_link_skip, TestSkipReason.PROFILE_DISABLED))
            }

            if (stopAfterLink) {
                val reason = stopAfterLinkReason ?: "Link inattivo o sconosciuto"
                emitLog(context.getString(R.string.log_link_cable_disconnected))
                skipRemainingSections(reason)
                finishTest()
                return@flow
            }

            // 2) Network Config
            // Cooperative cancellation checkpoint
            coroutineContext.ensureActive()
            emitProgress(TestProgressKey.NETWORK_CONFIG, 30, "Network Config", "Configurazione rete in corso...")

            updateTypedSection(
                typedSections = typedSections,
                id = TestSectionId.NETWORK,
                status = TestSectionStatus.RUNNING,
                title = "Network"
            )
            emitSnapshot()
            emitLog(context.getString(R.string.log_network_starting, probe.testInterface))

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
                    emitLog(context.getString(R.string.log_network_pass, feedback.mode, feedback.interfaceName))
                }
                is StepResult.Failed -> {
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
                    emitLog(context.getString(R.string.log_network_fail, errorMessage ?: "unknown error"))
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
                    emitLog(context.getString(R.string.log_network_skip, networkResult.reason))
                }
            }

            // 3) TDR
            if (profile.runTdr && probe.tdrSupported) {
                // Cooperative cancellation checkpoint
                coroutineContext.ensureActive()
                emitProgress(TestProgressKey.TDR, 50, "TDR", "Test cavo in corso...")

                updateTypedSection(
                    typedSections = typedSections,
                    id = TestSectionId.TDR,
                    status = TestSectionStatus.RUNNING,
                    title = "TDR"
                )
                emitSnapshot()
                emitLog(context.getString(R.string.log_tdr_starting, probe.testInterface))

                when (val tdrResult = cableTestStep.run(testExecutionContext)) {
                    is StepResult.Success -> {
                        val cableTest = tdrResult.data
                        reportData.tdr += cableTest.entries
                        val evaluation = qualityPolicy.evaluateTdr(cableTest, profile)
                        if (evaluation.status == TestSectionStatus.FAIL) {
                            overallStatus = "FAIL"
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
                        emitLog(context.getString(R.string.log_tdr_status, evaluation.status, cableTest.entries.size))
                    }
                    is StepResult.Failed -> {
                        val isFatal = tdrResult.error is TestError.Unsupported
                        if (!isFatal) overallStatus = "FAIL"
                        val status = if (isFatal) TestSectionStatus.SKIP else TestSectionStatus.FAIL
                        val message = tdrResult.error.message
                        recordStep(
                            id = TestSectionId.TDR,
                            title = "TDR",
                            status = status,
                            warning = message ?: TestSkipReason.HARDWARE_UNSUPPORTED,
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emitSnapshot()
                        val statusLabel = if (isFatal) "SKIP" else "FAIL"
                        emitLog(context.getString(R.string.log_tdr_fail, statusLabel, message ?: "unknown error"))
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
                        emitLog(context.getString(R.string.log_tdr_skip, tdrResult.reason))
                    }
                }
            } else if (profile.runTdr && !probe.tdrSupported) {
                recordStep(
                    id = TestSectionId.TDR,
                    title = "TDR",
                    status = TestSectionStatus.SKIP,
                    warning = TestSkipReason.HARDWARE_UNSUPPORTED,
                    rawData = mapOf("reason" to TestSkipReason.HARDWARE_UNSUPPORTED)
                )
                emitSnapshot()
                emitLog(context.getString(R.string.log_tdr_skip, TestSkipReason.HARDWARE_UNSUPPORTED))
            } else {
                recordStep(
                    id = TestSectionId.TDR,
                    title = "TDR",
                    status = TestSectionStatus.SKIP,
                    warning = TestSkipReason.PROFILE_DISABLED,
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSnapshot()
                emitLog(context.getString(R.string.log_tdr_skip, TestSkipReason.PROFILE_DISABLED))
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
                emitLog(context.getString(R.string.log_lldp_starting))

                when (val lldpResult = neighborDiscoveryStep.run(testExecutionContext)) {
                    is StepResult.Success -> {
                        val neighbors = lldpResult.data
                        reportData.neighbors += neighbors
                        recordStep(
                            id = TestSectionId.NEIGHBORS,
                            title = "LLDP/CDP",
                            status = TestSectionStatus.PASS,
                            rawData = lldpRaw(neighbors),
                            payload = TestSectionPayload.Neighbors(neighbors)
                        )
                        emitSnapshot()
                        emitLog(context.getString(R.string.log_lldp_pass, neighbors.size))
                    }
                    is StepResult.Failed -> {
                        val message = lldpResult.error.message
                        recordStep(
                            id = TestSectionId.NEIGHBORS,
                            title = "LLDP/CDP",
                            status = TestSectionStatus.INFO,
                            warning = message ?: "Unknown error",
                            rawData = mapOf("error" to message)
                        )
                        emitSnapshot()
                        emitLog(context.getString(R.string.log_lldp_info, message ?: "unknown error"))
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
                        emitLog(context.getString(R.string.log_lldp_skip, lldpResult.reason))
                    }
                }
            } else {
                recordStep(
                    id = TestSectionId.NEIGHBORS,
                    title = "LLDP/CDP",
                    status = TestSectionStatus.SKIP,
                    warning = TestSkipReason.PROFILE_DISABLED,
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSnapshot()
                emitLog(context.getString(R.string.log_lldp_skip, TestSkipReason.PROFILE_DISABLED))
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
                emitLog(context.getString(R.string.log_ping_starting))

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
                        emitLog(context.getString(R.string.log_ping_status, evaluation.status, outcomes.size, evaluation.warning?.let { " warn=$it" } ?: ""))
                    }
                    is StepResult.Failed -> {
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
                        emitLog(context.getString(R.string.log_ping_fail, error ?: "unknown error"))
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
                        emitLog(context.getString(R.string.log_ping_skip, pingResult.reason))
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
                emitLog(context.getString(R.string.log_ping_skip, TestSkipReason.PROFILE_DISABLED))
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
                emitLog(context.getString(R.string.log_speed_starting))

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
                        emitLog(context.getString(R.string.log_speed_status, evaluation.status, speed.tcpDownload ?: "-", speed.tcpUpload ?: "-", evaluation.warning?.let { " warn=$it" } ?: ""))
                    }
                    is StepResult.Failed -> {
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
                        emitLog(context.getString(R.string.log_speed_fail, message ?: "unknown error"))
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
                        emitLog(context.getString(R.string.log_speed_skip, speedResult.reason))
                    }
                }
            } else {
                recordStep(
                    id = TestSectionId.SPEED,
                    title = "Speed Test",
                    status = TestSectionStatus.SKIP,
                    warning = TestSkipReason.PROFILE_DISABLED,
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSnapshot()
                emitLog(context.getString(R.string.log_speed_skip, TestSkipReason.PROFILE_DISABLED))
            }

            finishTest()
        } catch (e: Exception) {
            emitLog(context.getString(R.string.log_result_error, e.message ?: "unknown error"))
            emit(TestEvent.Failed(TestError.Unexpected(e.message ?: "Unknown error", e)))
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
        return reportResultsCodec.encode(reportData).getOrElse { "{}" }
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
    sections += when {
        profile.runLinkStatus -> TestSectionSnapshot(id = TestSectionId.LINK, status = TestSectionStatus.PENDING, title = "Link")
        else -> TestSectionSnapshot(
            id = TestSectionId.LINK,
            status = TestSectionStatus.SKIP,
            title = "Link",
            warning = TestSkipReason.PROFILE_DISABLED
        )
    }
    sections += TestSectionSnapshot(
        id = TestSectionId.NETWORK,
        status = TestSectionStatus.PENDING,
        title = "Network"
    )
    sections += when {
        profile.runTdr && probe.tdrSupported -> TestSectionSnapshot(id = TestSectionId.TDR, status = TestSectionStatus.PENDING, title = "TDR")
        profile.runTdr && !probe.tdrSupported -> TestSectionSnapshot(
            id = TestSectionId.TDR,
            status = TestSectionStatus.SKIP,
            title = "TDR",
            warning = TestSkipReason.HARDWARE_UNSUPPORTED
        )
        else -> TestSectionSnapshot(
            id = TestSectionId.TDR,
            status = TestSectionStatus.SKIP,
            title = "TDR",
            warning = TestSkipReason.PROFILE_DISABLED
        )
    }
    sections += when {
        profile.runLldp -> TestSectionSnapshot(id = TestSectionId.NEIGHBORS, status = TestSectionStatus.PENDING, title = "LLDP/CDP")
        else -> TestSectionSnapshot(
            id = TestSectionId.NEIGHBORS,
            status = TestSectionStatus.SKIP,
            title = "LLDP/CDP",
            warning = TestSkipReason.PROFILE_DISABLED
        )
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
    sections += when {
        profile.runSpeedTest -> TestSectionSnapshot(id = TestSectionId.SPEED, status = TestSectionStatus.PENDING, title = "Speed Test")
        else -> TestSectionSnapshot(
            id = TestSectionId.SPEED,
            status = TestSectionStatus.SKIP,
            title = "Speed Test",
            warning = TestSkipReason.PROFILE_DISABLED
        )
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


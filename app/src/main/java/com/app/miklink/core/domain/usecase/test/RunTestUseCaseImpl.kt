package com.app.miklink.core.domain.usecase.test

import com.app.miklink.core.data.remote.mikrotik.dto.CableTestResult
import com.app.miklink.core.data.remote.mikrotik.dto.MonitorResponse
import com.app.miklink.core.data.remote.mikrotik.dto.NeighborDetail
import com.app.miklink.core.data.remote.mikrotik.dto.SpeedTestResult
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestProgress
import com.app.miklink.core.domain.test.model.TestSectionResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.step.CableTestStep
import com.app.miklink.core.domain.test.step.LinkStatusStep
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import com.app.miklink.core.domain.test.step.PingStep
import com.app.miklink.core.domain.test.step.SpeedTestStep
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementazione di RunTestUseCase.
 * Orchestra tutti gli step necessari per eseguire un test completo.
 * 
 * Ordine degli step (replicato da TestViewModel):
 * 1. Network Config
 * 2. Link Status
 * 3. TDR
 * 4. LLDP
 * 5. Ping
 * 6. Speed Test
 */
class RunTestUseCaseImpl @Inject constructor(
    private val clientRepository: ClientRepository,
    private val probeRepository: ProbeRepository,
    private val testProfileRepository: TestProfileRepository,
    private val networkConfigStep: NetworkConfigStep,
    private val linkStatusStep: LinkStatusStep,
    private val cableTestStep: CableTestStep,
    private val neighborDiscoveryStep: NeighborDiscoveryStep,
    private val pingStep: PingStep,
    private val speedTestStep: SpeedTestStep,
    private val moshi: Moshi
) : RunTestUseCase {

    private val rawResultsAdapter: JsonAdapter<RawTestResults> = moshi.adapter(RawTestResults::class.java)

    override fun execute(plan: TestPlan): Flow<TestEvent> = flow {
        // 1. Carica entità
        val client = clientRepository.getClient(plan.clientId)
            ?: throw IllegalStateException("Client not found: ${plan.clientId}")
        val probe = probeRepository.getProbe(plan.probeId)
            ?: throw IllegalStateException("Probe not found: ${plan.probeId}")
        val profile = testProfileRepository.getProfile(plan.profileId)
            ?: throw IllegalStateException("Profile not found: ${plan.profileId}")

        val context = TestExecutionContext(
            client = client,
            probeConfig = probe,
            profile = profile,
            socketId = plan.socketId,
            notes = plan.notes
        )

        emit(TestEvent.LogLine("--- INIZIO TEST ---"))
        emit(TestEvent.Progress(TestProgress("Inizializzazione", 0, "Caricamento dati...")))

        val sections = mutableListOf<TestSectionResult>()
        val rawSteps = mutableListOf<RawStep>()
        var overallStatus = "PASS"

        fun recordStep(
            name: String,
            title: String,
            status: String,
            details: Map<String, String> = emptyMap(),
            rawData: Map<String, Any?>? = null,
            error: String? = null
        ) {
            sections.add(TestSectionResult(type = name, title = title, status = status, details = details))
            rawSteps.add(RawStep(name = name, status = status, data = rawData, error = error))
        }

        try {
            // 1) Network Config
            emit(TestEvent.LogLine("Applicazione configurazione rete..."))
            emit(TestEvent.Progress(TestProgress("Network Config", 10, "Configurazione rete in corso...")))
            
            val networkResult = networkConfigStep.run(context)
            when (networkResult) {
                is StepResult.Success -> {
                    val feedback = networkResult.data as NetworkConfigFeedback
                    recordStep(
                        name = "NETWORK",
                        title = "Network",
                        status = "PASS",
                        details = networkDetails(feedback),
                        rawData = networkRaw(feedback)
                    )
                    emit(TestEvent.LogLine("Rete configurata con successo"))
                }
                is StepResult.Failed -> {
                    overallStatus = "FAIL"
                    val errorMessage = networkResult.error.message
                    recordStep(
                        name = "NETWORK",
                        title = "Network",
                        status = "FAIL",
                        details = mapOf("error" to (errorMessage ?: "Unknown error")),
                        error = errorMessage
                    )
                    emit(TestEvent.LogLine("Configurazione rete fallita: $errorMessage"))
                }
                is StepResult.Skipped -> {
                    recordStep(
                        name = "NETWORK",
                        title = "Network",
                        status = "SKIP",
                        details = mapOf("reason" to networkResult.reason),
                        rawData = mapOf("reason" to networkResult.reason)
                    )
                }
            }

            // 2) Link Status
            if (profile.runLinkStatus) {
                emit(TestEvent.LogLine("Esecuzione Test Stato Link..."))
                emit(TestEvent.Progress(TestProgress("Link Status", 30, "Verifica stato link...")))

                val linkResult = linkStatusStep.run(context)
                when (linkResult) {
                    is StepResult.Success -> {
                        val monitor = linkResult.data as MonitorResponse
                        recordStep(
                            name = "LINK",
                            title = "Link",
                            status = "PASS",
                            details = linkDetails(monitor),
                            rawData = linkRaw(monitor)
                        )
                        emit(TestEvent.LogLine("Link Status: OK"))
                    }
                    is StepResult.Failed -> {
                        overallStatus = "FAIL"
                        val errorMessage = linkResult.error.message
                        recordStep(
                            name = "LINK",
                            title = "Link",
                            status = "FAIL",
                            details = mapOf("error" to (errorMessage ?: "Errore sconosciuto")),
                            error = errorMessage
                        )
                        emit(TestEvent.LogLine("Link Status: FALLITO"))
                        // Stop immediato su errore link
                        emit(TestEvent.Failed(linkResult.error))
                        return@flow
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = "LINK",
                            title = "Link",
                            status = "SKIP",
                            details = mapOf("reason" to linkResult.reason),
                            rawData = mapOf("reason" to linkResult.reason)
                        )
                    }
                }
            } else {
                recordStep(
                    name = "LINK",
                    title = "Link",
                    status = "SKIP",
                    details = mapOf("reason" to "Profilo: runLinkStatus=false"),
                    rawData = mapOf("reason" to "disabled")
                )
            }

            // 3) TDR
            if (profile.runTdr && probe.tdrSupported) {
                emit(TestEvent.LogLine("Esecuzione TDR (Cable-Test)..."))
                emit(TestEvent.Progress(TestProgress("TDR", 50, "Test cavo in corso...")))

                val tdrResult = cableTestStep.run(context)
                when (tdrResult) {
                    is StepResult.Success -> {
                        val cableTest = tdrResult.data as CableTestResult
                        recordStep(
                            name = "TDR",
                            title = "TDR",
                            status = "PASS",
                            details = tdrDetails(cableTest),
                            rawData = tdrRaw(cableTest)
                        )
                        emit(TestEvent.LogLine("TDR: SUCCESSO"))
                    }
                    is StepResult.Failed -> {
                        // Non bloccare il test se TDR fallisce per incompatibilità hardware
                        val isFatal = tdrResult.error is TestError.Unsupported
                        if (!isFatal) overallStatus = "FAIL"
                        val status = if (isFatal) "SKIP" else "FAIL"
                        val message = tdrResult.error.message
                        recordStep(
                            name = "TDR",
                            title = "TDR",
                            status = status,
                            details = mapOf("error" to (message ?: "Errore sconosciuto")),
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emit(TestEvent.LogLine("TDR: ${if (isFatal) "NON SUPPORTATO" else "FALLITO"}"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = "TDR",
                            title = "TDR",
                            status = "SKIP",
                            details = mapOf("reason" to tdrResult.reason),
                            rawData = mapOf("reason" to tdrResult.reason)
                        )
                    }
                }
            } else if (profile.runTdr && !probe.tdrSupported) {
                emit(TestEvent.LogLine("TDR: SALTATO (hardware non supporta TDR)"))
                recordStep(
                    name = "TDR",
                    title = "TDR",
                    status = "SKIP",
                    details = mapOf("reason" to "Hardware non supporta TDR"),
                    rawData = mapOf("reason" to "Hardware non supporta TDR")
                )
            } else {
                recordStep(
                    name = "TDR",
                    title = "TDR",
                    status = "SKIP",
                    details = mapOf("reason" to "Profilo: runTdr=false"),
                    rawData = mapOf("reason" to "disabled")
                )
            }

            // 4) LLDP
            if (profile.runLldp) {
                emit(TestEvent.LogLine("Esecuzione discovery LLDP/CDP..."))
                emit(TestEvent.Progress(TestProgress("LLDP", 60, "Discovery neighbor...")))

                val lldpResult = neighborDiscoveryStep.run(context)
                when (lldpResult) {
                    is StepResult.Success -> {
                        val neighbors = lldpResult.data as List<*>
                        recordStep(
                            name = "LLDP",
                            title = "LLDP/CDP",
                            status = "PASS",
                            details = lldpDetails(neighbors),
                            rawData = lldpRaw(neighbors)
                        )
                        emit(TestEvent.LogLine("LLDP/CDP: Rilevato neighbor"))
                    }
                    is StepResult.Failed -> {
                        val message = lldpResult.error.message
                        recordStep(
                            name = "LLDP",
                            title = "LLDP/CDP",
                            status = "INFO",
                            details = mapOf("error" to (message ?: "Errore sconosciuto")),
                            rawData = mapOf("error" to message)
                        )
                        emit(TestEvent.LogLine("LLDP/CDP: FALLITO (${lldpResult.error.message})"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = "LLDP",
                            title = "LLDP/CDP",
                            status = "SKIP",
                            details = mapOf("reason" to lldpResult.reason),
                            rawData = mapOf("reason" to lldpResult.reason)
                        )
                    }
                }
            } else {
                recordStep(
                    name = "LLDP",
                    title = "LLDP/CDP",
                    status = "SKIP",
                    details = mapOf("reason" to "Profilo: runLldp=false"),
                    rawData = mapOf("reason" to "disabled")
                )
            }

            // 5) Ping
            if (profile.runPing) {
                emit(TestEvent.LogLine("Esecuzione Ping..."))
                emit(TestEvent.Progress(TestProgress("Ping", 70, "Test ping in corso...")))

                val pingResult = pingStep.run(context)
                when (pingResult) {
                    is StepResult.Success -> {
                        val outcomes = pingResult.data as List<*>
                        recordStep(
                            name = "PING",
                            title = "Ping",
                            status = "PASS",
                            details = pingDetails(outcomes),
                            rawData = pingRaw(outcomes)
                        )
                        emit(TestEvent.LogLine("Ping: SUCCESSO"))
                    }
                    is StepResult.Failed -> {
                        overallStatus = "FAIL"
                        val message = pingResult.error.message
                        recordStep(
                            name = "PING",
                            title = "Ping",
                            status = "FAIL",
                            details = mapOf("error" to (message ?: "Errore sconosciuto")),
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emit(TestEvent.LogLine("Ping: FALLITO"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = "PING",
                            title = "Ping",
                            status = "SKIP",
                            details = mapOf("reason" to pingResult.reason),
                            rawData = mapOf("reason" to pingResult.reason)
                        )
                    }
                }
            } else {
                recordStep(
                    name = "PING",
                    title = "Ping",
                    status = "SKIP",
                    details = mapOf("reason" to "Profilo: runPing=false"),
                    rawData = mapOf("reason" to "disabled")
                )
            }

            // 6) Speed Test
            if (profile.runSpeedTest) {
                emit(TestEvent.LogLine("Esecuzione Speed Test..."))
                emit(TestEvent.Progress(TestProgress("Speed Test", 90, "Speed test in corso...")))

                val speedResult = speedTestStep.run(context)
                when (speedResult) {
                    is StepResult.Success -> {
                        val speed = speedResult.data as SpeedTestResult
                        recordStep(
                            name = "SPEED",
                            title = "Speed Test",
                            status = "PASS",
                            details = speedDetails(speed, client.speedTestServerAddress),
                            rawData = speedRaw(speed, client.speedTestServerAddress)
                        )
                        emit(TestEvent.LogLine("Speed Test: COMPLETATO"))
                    }
                    is StepResult.Failed -> {
                        // Non fail l'intero test per uno speed test fallito (opzionale)
                        val message = speedResult.error.message
                        recordStep(
                            name = "SPEED",
                            title = "Speed Test",
                            status = "FAIL",
                            details = mapOf("error" to (message ?: "Errore sconosciuto")),
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emit(TestEvent.LogLine("Speed Test: FALLITO"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = "SPEED",
                            title = "Speed Test",
                            status = "SKIP",
                            details = mapOf("reason" to speedResult.reason),
                            rawData = mapOf("reason" to speedResult.reason)
                        )
                    }
                }
            } else {
                recordStep(
                    name = "SPEED",
                    title = "Speed Test",
                    status = "SKIP",
                    details = mapOf("reason" to "Profilo: runSpeedTest=false"),
                    rawData = mapOf("reason" to "disabled")
                )
            }

            emit(TestEvent.LogLine("--- TEST COMPLETATO ---"))
            emit(TestEvent.Progress(TestProgress("Completato", 100, "Test completato")))

            val outcome = TestOutcome(
                overallStatus = overallStatus,
                sections = sections,
                rawResultsJson = buildRawResults(plan, rawSteps)
            )

            emit(TestEvent.Completed(outcome))

        } catch (e: Exception) {
            emit(TestEvent.LogLine("ERRORE IRREVERSIBILE: ${e.message}"))
            emit(TestEvent.Failed(TestError.Unexpected(e.message ?: "Unknown error", e)))
        }
    }

    private fun buildRawResults(plan: TestPlan, rawSteps: List<RawStep>): String {
        val payload = RawTestResults(
            timestamp = System.currentTimeMillis(),
            plan = RawPlan(
                clientId = plan.clientId,
                probeId = plan.probeId,
                profileId = plan.profileId,
                socketId = plan.socketId
            ),
            steps = rawSteps
        )
        return try {
            rawResultsAdapter.toJson(payload)
        } catch (_: Exception) {
            "{}"
        }
    }

    private fun networkDetails(feedback: NetworkConfigFeedback): Map<String, String> =
        linkedMapOf(
            "mode" to feedback.mode,
            "interface" to feedback.interfaceName,
            "address" to (feedback.address ?: "-"),
            "gateway" to (feedback.gateway ?: "-"),
            "dns" to (feedback.dns ?: "-"),
            "message" to feedback.message
        )

    private fun networkRaw(feedback: NetworkConfigFeedback): Map<String, Any?> =
        linkedMapOf(
            "mode" to feedback.mode,
            "interface" to feedback.interfaceName,
            "address" to feedback.address,
            "gateway" to feedback.gateway,
            "dns" to feedback.dns,
            "message" to feedback.message
        )

    private fun linkDetails(response: MonitorResponse): Map<String, String> =
        linkedMapOf(
            "status" to response.status,
            "rate" to (response.rate ?: "-")
        )

    private fun linkRaw(response: MonitorResponse): Map<String, Any?> =
        linkedMapOf(
            "status" to response.status,
            "rate" to response.rate
        )

    private fun tdrDetails(result: CableTestResult): Map<String, String> =
        linkedMapOf(
            "status" to result.status,
            "pairs" to (result.cablePairs?.size?.toString() ?: "0")
        )

    private fun tdrRaw(result: CableTestResult): Map<String, Any?> =
        linkedMapOf(
            "status" to result.status,
            "cablePairs" to result.cablePairs
        )

    private fun lldpDetails(neighbors: List<*>): Map<String, String> {
        val first = neighbors.firstOrNull() as? NeighborDetail
        return linkedMapOf(
            "count" to neighbors.size.toString(),
            "identity" to (first?.identity ?: "-"),
            "interface" to (first?.interfaceName ?: "-"),
            "protocol" to (first?.discoveredBy ?: "-")
        )
    }

    private fun lldpRaw(neighbors: List<*>): Map<String, Any?> =
        linkedMapOf(
            "neighbors" to neighbors
        )

    private fun pingDetails(results: List<*>): Map<String, String> {
        val outcomes = results.filterIsInstance<PingTargetOutcome>()
        if (outcomes.isEmpty()) return mapOf("status" to "Nessun target valido")
        val summary = outcomes.joinToString("; ") { outcome ->
            val status = when {
                outcome.error != null -> "ERR"
                outcome.packetLoss == null -> "SKIP"
                outcome.packetLoss.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.let { it > 0.0 } == true -> "LOSS"
                else -> "OK"
            }
            "${outcome.target}:$status"
        }
        return linkedMapOf("targets" to summary)
    }

    private fun pingRaw(results: List<*>): Map<String, Any?> =
        linkedMapOf(
            "targets" to results
        )

    private fun speedDetails(speed: SpeedTestResult, serverAddress: String?): Map<String, String> =
        linkedMapOf(
            "server" to (serverAddress ?: "-"),
            "tcpDownload" to (speed.tcpDownload ?: "-"),
            "tcpUpload" to (speed.tcpUpload ?: "-"),
            "udpDownload" to (speed.udpDownload ?: "-"),
            "udpUpload" to (speed.udpUpload ?: "-"),
            "ping" to (speed.ping ?: "-"),
            "jitter" to (speed.jitter ?: "-"),
            "loss" to (speed.loss ?: "-"),
            "warning" to (speed.warning ?: "")
        ).filterValues { it.isNotEmpty() }

    private fun speedRaw(speed: SpeedTestResult, serverAddress: String?): Map<String, Any?> =
        linkedMapOf(
            "server" to serverAddress,
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

private data class RawPlan(
    val clientId: Long,
    val probeId: Long,
    val profileId: Long,
    val socketId: String?
)

private data class RawStep(
    val name: String,
    val status: String,
    val data: Map<String, Any?>? = null,
    val error: String? = null
)

private data class RawTestResults(
    val timestamp: Long,
    val plan: RawPlan,
    val steps: List<RawStep>
)


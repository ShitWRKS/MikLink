/*
 * Purpose: MikroTik test repository that binds Retrofit calls to the Wi-Fi network and returns domain data.
 * Inputs: Probe configuration plus per-call parameters (interface names, targets, credentials).
 * Outputs: Domain test models derived from MikroTik REST endpoints.
 * Notes: DTO usage stays internal; mapping is centralized in data/remote/mikrotik/mapper to keep ports clean.
 */
package com.app.miklink.data.repository.mikrotik

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.logging.DebugTraceRunContext
import com.app.miklink.core.domain.test.logging.DebugTraceSink
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.data.remote.mikrotik.dto.CableTestRequest
import com.app.miklink.data.remote.mikrotik.dto.MonitorRequest
import com.app.miklink.data.remote.mikrotik.dto.PingRequest
import com.app.miklink.data.remote.mikrotik.dto.SpeedTestRequest
import com.app.miklink.data.remote.mikrotik.mapper.RouterOsNormalizer
import com.app.miklink.data.remote.mikrotik.service.CallOutcome
import com.app.miklink.data.remote.mikrotik.service.DecodedResult
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.data.remote.mikrotik.service.RouterOsOperation
import com.app.miklink.data.remote.mikrotik.service.RouterOsResponseDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionException
import javax.inject.Inject
import java.util.UUID

/**
 * Implementazione remota di MikroTikTestRepository che usa MikroTikApiService.
 * Centralizza il fallback HTTPS->HTTP tramite MikroTikCallExecutor e il binding WiFi via service provider.
 */
class MikroTikTestRepositoryRemote @Inject constructor(
    private val callExecutor: MikroTikCallExecutor,
    private val decoder: RouterOsResponseDecoder,
    private val debugTraceSink: DebugTraceSink,
    private val debugTraceRunContext: DebugTraceRunContext
) : MikroTikTestRepository {

    override suspend fun monitorEthernet(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): LinkStatusData = withContext(Dispatchers.IO) {
        executeExchange("LINK", mapOf("interfaceName" to interfaceName, "once" to once)) {
        trace(
            event = "mikrotik_request",
            fields = mapOf(
                "test" to "LINK",
                "interfaceName" to interfaceName,
                "once" to once
            )
        )
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val response = api.getLinkStatus(MonitorRequest(numbers = interfaceName, once = once))
            val decoded = decoder.decode(RouterOsOperation.LINK_STATUS, response)
            when (decoded) {
                is DecodedResult.Error -> throw TestExecutionException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "probe_response",
                        fields = mapOf("test" to "LINK", "raw" to decoded.value)
                    )
                    trace("parsed_response", mapOf("test" to "LINK", "parsed" to decoded.value))
                    val latest = decoded.value.lastOrNull()
                        ?: throw TestExecutionException(TestError.InvalidResponse("No link status returned"))
                    val parsed = RouterOsNormalizer.normalizeLinkStatus(latest)
                    trace(
                        event = "normalized_response",
                        fields = mapOf("test" to "LINK", "parsed" to parsed)
                    )
                    parsed
                }
            }
        }
            outcome.getOrThrow(callExecutor)
        }
    }

    override suspend fun cableTest(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): CableTestSummary = withContext(Dispatchers.IO) {
        executeExchange("TDR", mapOf("interfaceName" to interfaceName, "once" to once)) {
        trace(
            event = "mikrotik_request",
            fields = mapOf(
                "test" to "TDR",
                "interfaceName" to interfaceName,
                "once" to once
            )
        )
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val response = api.runCableTest(CableTestRequest(numbers = interfaceName, duration = "5s"))
            val decoded = decoder.decode(RouterOsOperation.CABLE_TEST, response)
            when (decoded) {
                is DecodedResult.Error -> throw TestExecutionException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "probe_response",
                        fields = mapOf("test" to "TDR", "raw" to decoded.value)
                    )
                    trace("parsed_response", mapOf("test" to "TDR", "parsed" to decoded.value))
                    val validResult = decoded.value.lastOrNull {
                        it.cablePairs != null || it.status.lowercase() in listOf("ok", "open", "link-ok", "running")
                    } ?: throw TestExecutionException(TestError.InvalidResponse("No valid cable test results found"))
                    val parsed = RouterOsNormalizer.normalizeCableTest(validResult)
                    trace(
                        event = "normalized_response",
                        fields = mapOf("test" to "TDR", "parsed" to parsed)
                    )
                    parsed
                }
            }
        }
            outcome.getOrThrow(callExecutor)
        }
    }

    override suspend fun ping(
        probe: ProbeConfig,
        target: String,
        interfaceName: String?,
        count: Int
    ): List<PingMeasurement> = withContext(Dispatchers.IO) {
        executeExchange("PING", mapOf("target" to target, "interfaceName" to interfaceName, "count" to count)) {
        trace(
            event = "mikrotik_request",
            fields = mapOf(
                "test" to "PING",
                "target" to target,
                "interfaceName" to interfaceName,
                "count" to count
            )
        )
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val raw = api.runPing(PingRequest(address = target, `interface` = interfaceName, count = count.toString()))
            val decoded = decoder.decode(RouterOsOperation.PING, raw)
            when (decoded) {
                is DecodedResult.Error -> throw TestExecutionException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "probe_response",
                        fields = mapOf("test" to "PING", "target" to target, "raw" to decoded.value)
                    )
                    trace("parsed_response", mapOf("test" to "PING", "target" to target, "parsed" to decoded.value))
                    decoded.value.map { RouterOsNormalizer.normalizePing(it) }.also { parsed ->
                        trace(
                            event = "normalized_response",
                            fields = mapOf("test" to "PING", "target" to target, "parsed" to parsed)
                        )
                    }
                }
            }
        }
            outcome.getOrThrow(callExecutor)
        }
    }

    override suspend fun neighbors(
        probe: ProbeConfig,
        interfaceName: String
    ): List<NeighborData> = withContext(Dispatchers.IO) {
        executeExchange("NEIGHBORS", mapOf("interfaceName" to interfaceName)) {
        trace(
            event = "mikrotik_request",
            fields = mapOf(
                "test" to "NEIGHBORS",
                "interfaceName" to interfaceName
            )
        )
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val raw = api.getIpNeighbors(interfaceName)
            val decoded = decoder.decode(RouterOsOperation.NEIGHBORS, raw)
            when (decoded) {
                is DecodedResult.Error -> throw TestExecutionException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "probe_response",
                        fields = mapOf("test" to "NEIGHBORS", "raw" to decoded.value)
                    )
                    trace("parsed_response", mapOf("test" to "NEIGHBORS", "parsed" to decoded.value))
                    decoded.value.map { RouterOsNormalizer.normalizeNeighbor(it) }.also { parsed ->
                        trace(
                            event = "normalized_response",
                            fields = mapOf("test" to "NEIGHBORS", "parsed" to parsed)
                        )
                    }
                }
            }
        }
            outcome.getOrThrow(callExecutor)
        }
    }

    override suspend fun speedTest(
        probe: ProbeConfig,
        serverAddress: String,
        username: String?,
        password: String?,
        duration: String
    ): SpeedTestData = withContext(Dispatchers.IO) {
        executeExchange("SPEED", mapOf("serverAddress" to serverAddress, "duration" to duration)) {
        trace(
            event = "mikrotik_request",
            fields = mapOf(
                "test" to "SPEED",
                "serverAddress" to serverAddress,
                "duration" to duration
            )
        )
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val requestBody = SpeedTestRequest(
                address = serverAddress,
                user = username ?: "admin",
                password = password ?: "",
                testDuration = duration
            )
            val response = api.runSpeedTest(requestBody)
            val decoded = decoder.decode(RouterOsOperation.SPEED_TEST, response)
            when (decoded) {
                is DecodedResult.Error -> throw TestExecutionException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "probe_response",
                        fields = mapOf(
                            "test" to "SPEED",
                            "code" to response.code(),
                            "message" to response.message(),
                            "raw" to decoded.value
                        )
                    )
                    trace("parsed_response", mapOf("test" to "SPEED", "parsed" to decoded.value))
                    val body = decoded.value
                    val result = body.lastOrNull { it.status == "done" } ?: body.lastOrNull()
                    val parsed = result?.let { RouterOsNormalizer.normalizeSpeedTest(it, serverAddress) }
                        ?: throw TestExecutionException(TestError.InvalidResponse("Empty speed test response"))
                    trace(
                        event = "normalized_response",
                        fields = mapOf("test" to "SPEED", "parsed" to parsed)
                    )
                    parsed
                }
            }
        }
            outcome.getOrThrow(callExecutor)
        }
    }

    private fun trace(event: String, fields: Map<String, Any?>) {
        val correlation = debugTraceRunContext.correlation() ?: return
        compatibleTraceEventTypes(event).forEach { eventType ->
            debugTraceSink.correlatedEvent(correlation = correlation, event = eventType, fields = fields)
        }
    }

    private suspend fun <T> executeExchange(
        operationId: String,
        requestFields: Map<String, Any?>,
        block: suspend () -> T
    ): T {
        val exchangeId = UUID.randomUUID().toString()
        debugTraceRunContext.withOperation(operationId, exchangeId)
        trace("probe_request", mapOf("operation" to operationId) + requestFields)
        return try {
            block().also {
                trace("probe_exchange_completed", mapOf("operation" to operationId, "outcome" to "success"))
            }
        } catch (failure: Throwable) {
            trace(
                "probe_error",
                mapOf(
                    "operation" to operationId,
                    "type" to failure::class.java.simpleName,
                    "message" to failure.message
                )
            )
            throw failure
        }
    }
}

internal fun compatibleTraceEventTypes(event: String): List<String> =
    if (event == "probe_response") {
        // Transitional alias consumed by the unchanged legacy live-probe runners.
        listOf(event, "mikrotik_raw_response")
    } else {
        listOf(event)
    }

private fun <T> CallOutcome<T>.getOrThrow(executor: MikroTikCallExecutor): T {
    return when (this) {
        is CallOutcome.Success -> value
        is CallOutcome.Failure -> {
            val primary = failures.firstOrNull()?.throwable
                ?: IllegalStateException("Unknown call failure")
            failures.drop(1).forEach { primary.addSuppressed(it.throwable) }
            throw TestExecutionException(executor.classify(primary))
        }
    }
}

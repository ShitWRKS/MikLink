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
import java.io.IOException
import javax.inject.Inject

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
                is DecodedResult.Error -> throw mapToTransportException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "mikrotik_raw_response",
                        fields = mapOf("test" to "LINK", "raw" to decoded.value)
                    )
                    val latest = decoded.value.lastOrNull()
                        ?: throw IllegalStateException("No link status returned")
                    val parsed = RouterOsNormalizer.normalizeLinkStatus(latest)
                    trace(
                        event = "parsed_response",
                        fields = mapOf("test" to "LINK", "parsed" to parsed)
                    )
                    parsed
                }
            }
        }
        outcome.getOrThrow(callExecutor)
    }

    override suspend fun cableTest(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): CableTestSummary = withContext(Dispatchers.IO) {
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
                is DecodedResult.Error -> throw mapToTransportException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "mikrotik_raw_response",
                        fields = mapOf("test" to "TDR", "raw" to decoded.value)
                    )
                    val validResult = decoded.value.lastOrNull {
                        it.cablePairs != null || it.status.lowercase() in listOf("ok", "open", "link-ok", "running")
                    } ?: throw IllegalStateException("No valid cable test results found")
                    val parsed = RouterOsNormalizer.normalizeCableTest(validResult)
                    trace(
                        event = "parsed_response",
                        fields = mapOf("test" to "TDR", "parsed" to parsed)
                    )
                    parsed
                }
            }
        }
        outcome.getOrThrow(callExecutor)
    }

    override suspend fun ping(
        probe: ProbeConfig,
        target: String,
        interfaceName: String?,
        count: Int
    ): List<PingMeasurement> = withContext(Dispatchers.IO) {
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
                is DecodedResult.Error -> throw mapToTransportException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "mikrotik_raw_response",
                        fields = mapOf("test" to "PING", "target" to target, "raw" to decoded.value)
                    )
                    decoded.value.map { RouterOsNormalizer.normalizePing(it) }.also { parsed ->
                        trace(
                            event = "parsed_response",
                            fields = mapOf("test" to "PING", "target" to target, "parsed" to parsed)
                        )
                    }
                }
            }
        }
        outcome.getOrThrow(callExecutor)
    }

    override suspend fun neighbors(
        probe: ProbeConfig,
        interfaceName: String
    ): List<NeighborData> = withContext(Dispatchers.IO) {
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
                is DecodedResult.Error -> throw mapToTransportException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "mikrotik_raw_response",
                        fields = mapOf("test" to "NEIGHBORS", "raw" to decoded.value)
                    )
                    decoded.value.map { RouterOsNormalizer.normalizeNeighbor(it) }.also { parsed ->
                        trace(
                            event = "parsed_response",
                            fields = mapOf("test" to "NEIGHBORS", "parsed" to parsed)
                        )
                    }
                }
            }
        }
        outcome.getOrThrow(callExecutor)
    }

    override suspend fun speedTest(
        probe: ProbeConfig,
        serverAddress: String,
        username: String?,
        password: String?,
        duration: String
    ): SpeedTestData = withContext(Dispatchers.IO) {
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
                is DecodedResult.Error -> throw mapToTransportException(decoded.error)
                is DecodedResult.Success -> {
                    trace(
                        event = "mikrotik_raw_response",
                        fields = mapOf(
                            "test" to "SPEED",
                            "code" to response.code(),
                            "message" to response.message(),
                            "raw" to decoded.value
                        )
                    )
                    val body = decoded.value
                    val result = body.lastOrNull { it.status == "done" } ?: body.lastOrNull()
                    val parsed = result?.let { RouterOsNormalizer.normalizeSpeedTest(it, serverAddress) }
                        ?: throw IllegalStateException("Empty speed test response")
                    trace(
                        event = "parsed_response",
                        fields = mapOf("test" to "SPEED", "parsed" to parsed)
                    )
                    parsed
                }
            }
        }
        outcome.getOrThrow(callExecutor)
    }

    private fun trace(event: String, fields: Map<String, Any?>) {
        val runId = debugTraceRunContext.current() ?: return
        debugTraceSink.event(runId = runId, event = event, fields = fields)
    }
}

private fun mapToTransportException(error: com.app.miklink.core.domain.test.model.TestError): Exception {
    return when (error) {
        is com.app.miklink.core.domain.test.model.TestError.ProbeUnavailable ->
            IOException(error.message, error.cause)
        is com.app.miklink.core.domain.test.model.TestError.Authentication ->
            SecurityException(error.message)
        is com.app.miklink.core.domain.test.model.TestError.Tls ->
            javax.net.ssl.SSLHandshakeException(error.message)
        is com.app.miklink.core.domain.test.model.TestError.Timeout ->
            java.net.SocketTimeoutException(error.message)
        is com.app.miklink.core.domain.test.model.TestError.RouterOsError ->
            com.app.miklink.data.remote.mikrotik.service.RouterOsTransportException(error)
        is com.app.miklink.core.domain.test.model.TestError.InvalidResponse ->
            IllegalStateException(error.message)
        is com.app.miklink.core.domain.test.model.TestError.Unsupported ->
            UnsupportedOperationException(error.message)
        is com.app.miklink.core.domain.test.model.TestError.ConfigurationError ->
            IllegalStateException(error.message)
        is com.app.miklink.core.domain.test.model.TestError.SerializationError ->
            IllegalStateException(error.message, error.cause)
        is com.app.miklink.core.domain.test.model.TestError.Unexpected ->
            IllegalStateException(error.message, error.cause)
    }
}

private fun <T> CallOutcome<T>.getOrThrow(executor: MikroTikCallExecutor): T {
    return when (this) {
        is CallOutcome.Success -> value
        is CallOutcome.Failure -> {
            val primary = failures.firstOrNull()?.throwable ?: IllegalStateException("Unknown call failure")
            failures.drop(1).forEach { primary.addSuppressed(it.throwable) }
            throw mapToTransportException(executor.classify(primary))
        }
    }
}

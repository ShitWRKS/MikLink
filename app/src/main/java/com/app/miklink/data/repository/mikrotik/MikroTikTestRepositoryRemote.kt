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
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.data.remote.mikrotik.dto.CableTestRequest
import com.app.miklink.data.remote.mikrotik.dto.MonitorRequest
import com.app.miklink.data.remote.mikrotik.dto.PingRequest
import com.app.miklink.data.remote.mikrotik.dto.SpeedTestRequest
import com.app.miklink.data.remote.mikrotik.mapper.toDomain
import com.app.miklink.data.remote.mikrotik.mapper.toLinkStatusData
import com.app.miklink.data.remote.mikrotik.mapper.toMeasurement
import com.app.miklink.data.remote.mikrotik.mapper.toSummary
import com.app.miklink.data.remote.mikrotik.service.CallOutcome
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Implementazione remota di MikroTikTestRepository che usa MikroTikApiService.
 * Centralizza il fallback HTTPS->HTTP tramite MikroTikCallExecutor e il binding WiFi via service provider.
 */
class MikroTikTestRepositoryRemote @Inject constructor(
    private val callExecutor: MikroTikCallExecutor
) : MikroTikTestRepository {

    override suspend fun monitorEthernet(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): LinkStatusData = withContext(Dispatchers.IO) {
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val results = api.getLinkStatus(MonitorRequest(numbers = interfaceName, once = once))
            val latest = results.lastOrNull() ?: throw IllegalStateException("No link status returned")
            latest.toLinkStatusData()
        }
        outcome.getOrThrow()
    }

    override suspend fun cableTest(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): CableTestSummary = withContext(Dispatchers.IO) {
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val results = api.runCableTest(CableTestRequest(numbers = interfaceName, duration = "5s"))
            val validResult = results.lastOrNull {
                it.cablePairs != null || it.status.lowercase() in listOf("ok", "open", "link-ok", "running")
            } ?: throw IllegalStateException("No valid cable test results found")
            validResult.toSummary()
        }
        outcome.getOrThrow()
    }

    override suspend fun ping(
        probe: ProbeConfig,
        target: String,
        interfaceName: String?,
        count: Int
    ): List<PingMeasurement> = withContext(Dispatchers.IO) {
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            api.runPing(PingRequest(address = target, `interface` = interfaceName, count = count.toString()))
                .map { it.toMeasurement() }
        }
        outcome.getOrThrow()
    }

    override suspend fun neighbors(
        probe: ProbeConfig,
        interfaceName: String
    ): List<NeighborData> = withContext(Dispatchers.IO) {
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            api.getIpNeighbors(interfaceName).map { it.toDomain() }
        }
        outcome.getOrThrow()
    }

    override suspend fun speedTest(
        probe: ProbeConfig,
        serverAddress: String,
        username: String?,
        password: String?,
        duration: String
    ): SpeedTestData = withContext(Dispatchers.IO) {
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val requestBody = SpeedTestRequest(
                address = serverAddress,
                user = username ?: "admin",
                password = password ?: "",
                testDuration = duration
            )
            val response = api.runSpeedTest(requestBody)
            if (response.isSuccessful) {
                val body = response.body()
                val result = body?.lastOrNull { it.status == "done" } ?: body?.lastOrNull()
                result?.toDomain(serverAddress) ?: throw IllegalStateException("Empty speed test response")
            } else {
                when (response.code()) {
                    400 -> throw IllegalArgumentException("Bad request: ${response.message()}")
                    401, 403 -> throw SecurityException("Authentication failed")
                    else -> throw HttpException(response)
                }
            }
        }
        outcome.getOrThrow()
    }
}

private fun <T> CallOutcome<T>.getOrThrow(): T {
    return when (this) {
        is CallOutcome.Success -> value
        is CallOutcome.Failure -> {
            val primary = failures.firstOrNull()?.throwable ?: IllegalStateException("Unknown call failure")
            failures.drop(1).forEach { primary.addSuppressed(it.throwable) }
            throw primary
        }
    }
}

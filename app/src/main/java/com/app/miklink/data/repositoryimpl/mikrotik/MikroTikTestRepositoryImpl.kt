/*
 * Purpose: MikroTik test repository implementation that binds Retrofit calls to the Wi-Fi network and returns domain data.
 * Inputs: Probe configuration plus per-call parameters (interface names, targets, credentials).
 * Outputs: Domain test models derived from MikroTik REST endpoints.
 * Notes: DTO usage stays internal; mapping is centralized in data/remote/mikrotik/mapper to keep ports clean.
 */
package com.app.miklink.data.repositoryimpl.mikrotik

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
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Implementazione di MikroTikTestRepository che usa MikroTikApiService.
 * Centralizza il fallback HTTPS?HTTP tramite MikroTikCallExecutor e il binding WiFi via service provider.
 */
class MikroTikTestRepositoryImpl @Inject constructor(
    private val callExecutor: MikroTikCallExecutor
) : MikroTikTestRepository {

    override suspend fun monitorEthernet(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): LinkStatusData = withContext(Dispatchers.IO) {
        callExecutor.execute(probe) { api ->
            val results = api.getLinkStatus(MonitorRequest(numbers = interfaceName, once = once))
            val latest = results.lastOrNull() ?: throw IllegalStateException("No link status returned")
            latest.toLinkStatusData()
        }.value
    }

    override suspend fun cableTest(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): CableTestSummary = withContext(Dispatchers.IO) {
        callExecutor.execute(probe) { api ->
            val results = api.runCableTest(CableTestRequest(numbers = interfaceName, duration = "5s"))
            val validResult = results.lastOrNull {
                it.cablePairs != null || it.status.lowercase() in listOf("ok", "open", "link-ok", "running")
            } ?: throw IllegalStateException("No valid cable test results found")
            validResult.toSummary()
        }.value
    }

    override suspend fun ping(
        probe: ProbeConfig,
        target: String,
        interfaceName: String?,
        count: Int
    ): List<PingMeasurement> = withContext(Dispatchers.IO) {
        callExecutor.execute(probe) { api ->
            api.runPing(PingRequest(address = target, `interface` = interfaceName, count = count.toString()))
                .map { it.toMeasurement() }
        }.value
    }

    override suspend fun neighbors(
        probe: ProbeConfig,
        interfaceName: String
    ): List<NeighborData> = withContext(Dispatchers.IO) {
        callExecutor.execute(probe) { api ->
            api.getIpNeighbors(interfaceName).map { it.toDomain() }
        }.value
    }

    override suspend fun speedTest(
        probe: ProbeConfig,
        serverAddress: String,
        username: String?,
        password: String?,
        duration: String
    ): SpeedTestData = withContext(Dispatchers.IO) {
        callExecutor.execute(probe) { api ->
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
        }.value
    }
}

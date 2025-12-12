package com.app.miklink.core.data.repository

import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.local.room.v1.model.Report
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import com.app.miklink.core.data.remote.mikrotik.dto.CableTestResult
import com.app.miklink.core.data.remote.mikrotik.dto.MonitorResponse
import com.app.miklink.core.data.remote.mikrotik.dto.NeighborDetail
import com.app.miklink.core.data.remote.mikrotik.dto.PingResult
import com.app.miklink.core.data.remote.mikrotik.dto.SpeedTestResult
import com.app.miklink.utils.UiState
import kotlinx.coroutines.flow.Flow

/**
 * Bridge interface for the application repository. Implemented by legacy implementation
 * during migration. Define only signatures used by UI/ViewModels to keep DI stable.
 */
interface AppRepository {
    val currentProbe: Flow<ProbeConfig?>

    suspend fun applyClientNetworkConfig(probe: ProbeConfig, client: Client, override: Client? = null): UiState<NetworkConfigFeedback>

    suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult>

    suspend fun getLinkStatus(probe: ProbeConfig, interfaceName: String): UiState<MonitorResponse>

    suspend fun getNeighborsForInterface(probe: ProbeConfig, interfaceName: String): UiState<List<NeighborDetail>>

    suspend fun resolveTargetIp(probe: ProbeConfig, target: String, interfaceName: String): String

    suspend fun runPing(probe: ProbeConfig, target: String, interfaceName: String, count: Int = 4): UiState<List<PingResult>>

    suspend fun runSpeedTest(probe: ProbeConfig, client: Client): UiState<SpeedTestResult>

    fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>

    fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>

    suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult

}

data class ProbeStatusInfo(val probe: ProbeConfig, val isOnline: Boolean)

sealed class ProbeCheckResult {
    data class Success(val boardName: String, val interfaces: List<String>) : ProbeCheckResult()
    data class Error(val message: String) : ProbeCheckResult()
}

data class NetworkConfigFeedback(
    val mode: String,
    val interfaceName: String,
    val address: String?,
    val gateway: String?,
    val dns: String?,
    val message: String
)

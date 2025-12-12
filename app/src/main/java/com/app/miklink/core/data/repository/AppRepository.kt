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
 * 
 * @deprecated S5: I metodi di test (runCableTest, getLinkStatus, getNeighborsForInterface, runPing, runSpeedTest)
 * sono stati sostituiti da RunTestUseCase + Step implementations. Questa interfaccia resta per:
 * - applyClientNetworkConfig (usato da NetworkConfigRepository bridge)
 * - resolveTargetIp (usato da PingStep temporaneamente)
 * - observeAllProbesWithStatus, observeProbeStatus, checkProbeConnection (usati da altre feature)
 */
interface AppRepository {
    val currentProbe: Flow<ProbeConfig?>

    suspend fun applyClientNetworkConfig(probe: ProbeConfig, client: Client, override: Client? = null): UiState<NetworkConfigFeedback>

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + CableTestStep
     */
    @Deprecated("Use RunTestUseCase + CableTestStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult>

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + LinkStatusStep
     */
    @Deprecated("Use RunTestUseCase + LinkStatusStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun getLinkStatus(probe: ProbeConfig, interfaceName: String): UiState<MonitorResponse>

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + NeighborDiscoveryStep
     */
    @Deprecated("Use RunTestUseCase + NeighborDiscoveryStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun getNeighborsForInterface(probe: ProbeConfig, interfaceName: String): UiState<List<NeighborDetail>>

    suspend fun resolveTargetIp(probe: ProbeConfig, target: String, interfaceName: String): String

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + PingStep
     */
    @Deprecated("Use RunTestUseCase + PingStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun runPing(probe: ProbeConfig, target: String, interfaceName: String, count: Int = 4): UiState<List<PingResult>>

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + SpeedTestStep
     */
    @Deprecated("Use RunTestUseCase + SpeedTestStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
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

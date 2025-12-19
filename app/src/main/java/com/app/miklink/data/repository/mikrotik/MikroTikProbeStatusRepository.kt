/*
 * Purpose: Monitor probe online status using MikroTik REST calls and emit probe status updates.
 * Inputs: Stored ProbeConfig, MikroTikCallExecutor, and user preferences for polling interval.
 * Outputs: Flows indicating online/offline status for the singleton probe.
 * Notes: Single transport path via MikroTikCallExecutor; uses proplist filtering to avoid false positives.
 */
package com.app.miklink.data.repository.mikrotik

import com.app.miklink.core.data.repository.ProbeStatusInfo
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.probe.ProbeStatusRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.dto.ProplistRequest
import com.app.miklink.data.remote.mikrotik.service.CallOutcome
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Implementazione MikroTik di ProbeStatusRepository.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MikroTikProbeStatusRepository @Inject constructor(
    private val probeRepository: ProbeRepository,
    private val callExecutor: MikroTikCallExecutor,
    private val userPreferencesRepository: UserPreferencesRepository
) : ProbeStatusRepository {

    override fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean> =
        userPreferencesRepository.probePollingInterval
            .flatMapLatest { interval ->
                flow {
                    while (true) {
                        val isOnline = try {
                            val outcome = callExecutor.executeWithOutcome(probe) { api ->
                                api.getSystemResource(ProplistRequest(listOf("board-name")))
                                    .any { !it.boardName.isNullOrBlank() }
                            }
                            outcome.getOrThrow()
                        } catch (_: HttpException) {
                            false
                        } catch (_: Exception) {
                            false
                        }
                        emit(isOnline)
                        delay(interval)
                    }
                }
            }

    override fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>> =
        combine(probeRepository.observeProbeConfig(), userPreferencesRepository.probePollingInterval) { probe, interval ->
            probe to interval
        }.flatMapLatest { (probe, interval) ->
            if (probe == null) return@flatMapLatest flowOf(emptyList())
            tickerFlow(interval).map {
                withContext(Dispatchers.IO) {
                    val isOnline = try {
                        val outcome = callExecutor.executeWithOutcome(probe) { api ->
                            val result = api.getSystemResource(ProplistRequest(listOf("board-name")))
                            result.any { !it.boardName.isNullOrBlank() }
                        }
                        outcome.getOrThrow()
                    } catch (e: Exception) {
                        android.util.Log.w("ProbeStatusRepository", "Sonda @ ${probe.ipAddress} offline: ${e.message}")
                        false
                    }
                    listOf(ProbeStatusInfo(probe, isOnline))
                }
            }
        }
}

// Simple ticker flow helper
private fun tickerFlow(periodMs: Long): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(periodMs)
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

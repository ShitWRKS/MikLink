/*
 * Purpose: Monitor probe online status using MikroTik REST calls and emit probe status updates.
 * Inputs: Stored ProbeConfig, MikroTikServiceProvider for API access, and user preferences for polling interval.
 * Outputs: Flows indicating online/offline status for the singleton probe.
 * Notes: Uses `.proplist` filtering and ignores entries without board-name to avoid false positives.
 */
package com.app.miklink.data.repositoryimpl.mikrotik

import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.dto.ProplistRequest
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.core.data.repository.ProbeStatusInfo
import com.app.miklink.core.data.repository.probe.ProbeStatusRepository
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Implementazione di ProbeStatusRepository.
 *
 * Usa MikroTikServiceProvider per costruire il service e chiama l'API MikroTik
 * per verificare lo stato online/offline delle sonde.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProbeStatusRepositoryImpl @Inject constructor(
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
                            callExecutor.execute(probe) { api ->
                                api.getSystemResource(ProplistRequest(listOf("board-name")))
                                    .any { !it.boardName.isNullOrBlank() }
                            }.value
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
                        callExecutor.execute(probe) { api ->
                            val result = api.getSystemResource(ProplistRequest(listOf("board-name")))
                            result.any { !it.boardName.isNullOrBlank() }
                        }.value
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

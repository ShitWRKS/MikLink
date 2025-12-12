package com.app.miklink.data.repositoryimpl

import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.repository.AppRepository
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.test.NetworkConfigRepository
import com.app.miklink.utils.UiState
import javax.inject.Inject

/**
 * Implementazione di NetworkConfigRepository.
 *
 * Temporary bridge to AppRepository.applyClientNetworkConfig.
 * Usa esclusivamente AppRepository.applyClientNetworkConfig finché non verrà
 * introdotta una implementazione dedicata (rimozione prevista in EPIC S6).
 */
class NetworkConfigRepositoryImpl @Inject constructor(
    private val appRepository: AppRepository
) : NetworkConfigRepository {
    @Deprecated("Temporary bridge: replace with dedicated implementation")
    override suspend fun applyClientNetworkConfig(
        probe: ProbeConfig,
        client: Client,
        override: Client?
    ): NetworkConfigFeedback {
        val result = appRepository.applyClientNetworkConfig(probe, client, override)
        return when (result) {
            is UiState.Success -> result.data
            is UiState.Error -> throw IllegalStateException(result.message)
            else -> throw IllegalStateException("Unexpected state")
        }
    }
}


package com.app.miklink.core.data.repository.test

import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.repository.NetworkConfigFeedback

/**
 * Temporary bridge to the legacy AppRepository.
 *
 * Keeps applyClientNetworkConfig available while the runner is migrated.
 * Must be removed/replaced in EPIC S6 with a dedicated implementation.
 */
interface NetworkConfigRepository {
    @Deprecated("Temporary bridge: replace with dedicated implementation")
    suspend fun applyClientNetworkConfig(
        probe: ProbeConfig,
        client: Client,
        override: Client? = null
    ): NetworkConfigFeedback
}


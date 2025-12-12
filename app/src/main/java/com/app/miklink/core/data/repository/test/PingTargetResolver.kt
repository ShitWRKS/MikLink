package com.app.miklink.core.data.repository.test

import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.local.room.v1.model.TestProfile

/**
 * Temporary bridge used to resolve ping targets (e.g. DHCP gateway) outside AppRepository.
 *
 * Will be replaced with a dedicated implementation in EPIC S6.
 *
 * @param probe Required: needed to build MikroTik API service and call getDhcpClientStatus()
 *              to resolve DHCP gateway. The probe configuration contains connection details
 *              (host, port, credentials) necessary to establish the API connection.
 */
interface PingTargetResolver {
    suspend fun resolve(
        probe: ProbeConfig,
        client: Client,
        profile: TestProfile,
        input: String
    ): String
}



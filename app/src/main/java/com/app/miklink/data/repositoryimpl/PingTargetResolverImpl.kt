package com.app.miklink.data.repositoryimpl

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.data.repository.test.DhcpGatewayRepository
import com.app.miklink.core.data.repository.test.PingTargetResolver
import javax.inject.Inject

/**
 * Implementazione di PingTargetResolver.
 *
 * Usa DhcpGatewayRepository per risolvere il gateway DHCP (S6.3).
 * Non costruisce più direttamente il service né chiama direttamente l'API DHCP.
 */
class PingTargetResolverImpl @Inject constructor(
    private val dhcpGatewayRepository: DhcpGatewayRepository
) : PingTargetResolver {

    override suspend fun resolve(
        probe: ProbeConfig,
        client: Client,
        profile: TestProfile,
        input: String
    ): String {
        if (input.equals("DHCP_GATEWAY", ignoreCase = true)) {
            return dhcpGatewayRepository.getGatewayForInterface(probe, probe.testInterface) ?: input
        }
        return input
    }
}



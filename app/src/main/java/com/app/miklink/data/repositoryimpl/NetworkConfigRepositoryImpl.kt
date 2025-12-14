package com.app.miklink.data.repositoryimpl

import android.content.Context
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.data.remote.mikrotik.dto.*
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.test.NetworkConfigRepository
import com.app.miklink.data.repository.RouteManager
import com.app.miklink.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Implementazione di NetworkConfigRepository.
 *
 * Esegue direttamente le operazioni di configurazione rete senza dipendere da AppRepository.
 * Replica la logica di AppRepository.applyClientNetworkConfig mantenendo lo stesso comportamento.
 */
class NetworkConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serviceProvider: MikroTikServiceProvider,
    private val routeManager: RouteManager
) : NetworkConfigRepository {

    override suspend fun applyClientNetworkConfig(
        probe: ProbeConfig,
        client: Client,
        override: Client?
    ): NetworkConfigFeedback {
        val effective = override ?: client
        val api = serviceProvider.build(probe)
        val iface = probe.testInterface

        // Helper: rimuovi IP statici su interfaccia
        suspend fun removeStaticAddressesOnInterface() {
            val addresses = api.getIpAddresses()
            addresses.filter { it.iface == iface }.forEach { entry ->
                entry.id?.let { api.removeIpAddress(NumbersRequest(it)) }
            }
        }

        if (effective.networkMode == NetworkMode.DHCP) {
            // DHCP: verifica se già configurato e bound, altrimenti configura
            val existingDhcp = api.getDhcpClientStatus(iface).firstOrNull()

            // Se il client DHCP esiste ed è già bound, non fare nulla
            if (existingDhcp != null &&
                existingDhcp.disabled == "false" &&
                existingDhcp.status?.equals("bound", ignoreCase = true) == true) {
                // DHCP già configurato correttamente, ritorna lo stato attuale
                return NetworkConfigFeedback(
                    mode = "DHCP",
                    interfaceName = iface,
                    address = existingDhcp.address,
                    gateway = existingDhcp.gateway,
                    dns = existingDhcp.dns,
                    message = context.getString(R.string.status_dhcp_configured)
                )
            }

            // Altrimenti, configura il DHCP
            if (existingDhcp != null) {
                existingDhcp.id?.let { dhcpId ->
                    // Client esiste ma è disabilitato o non bound: riabilita
                    if (existingDhcp.disabled == "true") {
                        api.enableDhcpClient(NumbersRequest(dhcpId))
                    } else {
                        // Client abilitato ma non bound: disable/enable per refresh
                        api.disableDhcpClient(NumbersRequest(dhcpId))
                        delay(500)
                        api.enableDhcpClient(NumbersRequest(dhcpId))
                    }
                }
            } else {
                // Client non esiste: crea
                try {
                    api.addDhcpClient(DhcpClientAdd(`interface` = iface))
                } catch (e: Exception) {
                    // Se il client esiste già (race condition), recuperalo e abilitalo
                    if (e.message?.contains("already exists", ignoreCase = true) == true) {
                        delay(500)
                        val existingId = api.getDhcpClientStatus(iface).firstOrNull()?.id
                        if (existingId != null) {
                            api.enableDhcpClient(NumbersRequest(existingId))
                        } else {
                            throw e
                        }
                    } else {
                        throw e
                    }
                }
            }

            // Attendi lease (max 6 secondi)
            var lease: DhcpClientStatus? = null
            repeat(6) {
                val cur = api.getDhcpClientStatus(iface).firstOrNull()
                if (cur?.status?.equals("bound", true) == true) {
                    lease = cur
                    return@repeat
                }
                delay(1000)
            }
            val bound = lease ?: api.getDhcpClientStatus(iface).firstOrNull()
            return NetworkConfigFeedback(
                mode = "DHCP",
                interfaceName = iface,
                address = bound?.address,
                gateway = bound?.gateway,
                dns = bound?.dns,
                message = if (bound?.status == "bound") {
                    context.getString(R.string.status_dhcp_lease_active)
                } else {
                    context.getString(R.string.status_dhcp_not_bound)
                }
            )
        } else {
            // STATIC
            // Disabilita DHCP se presente
            val dhcpId = api.getDhcpClientStatus(iface).firstOrNull()?.id
            if (dhcpId != null) {
                api.disableDhcpClient(NumbersRequest(dhcpId))
            }
            removeStaticAddressesOnInterface()
            // Ensure we have gateway value for a better match
            val expectedGateway = effective.staticGateway
            routeManager.removeDefaultRoutes(api, expectedGateway)

            val cidr = effective.staticCidr ?: buildString {
                val ip = effective.staticIp ?: ""
                val mask = effective.staticSubnet ?: ""
                if (ip.isNotBlank() && mask.isNotBlank()) append("$ip/$mask")
            }
            require(!cidr.isNullOrBlank()) {
                context.getString(R.string.error_static_cidr_missing)
            }

            api.addIpAddress(IpAddressAdd(address = cidr, `interface` = iface))
            val gw = effective.staticGateway
                ?: error(context.getString(R.string.error_static_gateway_missing))
            api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gw, comment = "MikLink_Auto"))

            return NetworkConfigFeedback(
                mode = "STATIC",
                interfaceName = iface,
                address = cidr,
                gateway = gw,
                dns = null,
                message = context.getString(R.string.status_static_configured)
            )
        }
    }
}

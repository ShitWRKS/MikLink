/*
 * Purpose: Apply network configuration to MikroTik probes (DHCP/static) via centralized transport.
 * Inputs: ProbeConfig, Client settings, RouteManager, and MikroTikCallExecutor.
 * Outputs: NetworkConfigFeedback describing resulting network state.
 * Notes: Keeps HTTPS->HTTP fallback centralized; mirrors legacy AppRepository logic without duplicating transport.
 */
package com.app.miklink.data.repository.mikrotik

import android.content.Context
import com.app.miklink.R
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.test.NetworkConfigRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.RouterOsErrorCategory
import com.app.miklink.data.remote.mikrotik.dto.DhcpClientAdd
import com.app.miklink.data.remote.mikrotik.dto.DhcpClientStatus
import com.app.miklink.data.remote.mikrotik.dto.IpAddressAdd
import com.app.miklink.data.remote.mikrotik.dto.NumbersRequest
import com.app.miklink.data.remote.mikrotik.dto.RouteAdd
import com.app.miklink.data.remote.mikrotik.service.CallOutcome
import com.app.miklink.data.remote.mikrotik.service.DecodedResult
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.MikroTikCallExecutor
import com.app.miklink.data.remote.mikrotik.service.RouterOsOperation
import com.app.miklink.data.remote.mikrotik.service.RouterOsResponseDecoder
import com.app.miklink.core.domain.test.model.TestExecutionException
import com.app.miklink.data.repository.RouteManager
import retrofit2.Response
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Implementazione MikroTik di NetworkConfigRepository.
 */
class MikroTikNetworkConfigRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val callExecutor: MikroTikCallExecutor,
    private val decoder: RouterOsResponseDecoder,
    private val routeManager: RouteManager
) : NetworkConfigRepository {

    override suspend fun applyClientNetworkConfig(
        probe: ProbeConfig,
        client: Client,
        override: Client?
    ): NetworkConfigFeedback {
        val effective = override ?: client
        val outcome = callExecutor.executeWithOutcome(probe) { api ->
            val iface = probe.testInterface

            // Helper: rimuovi IP statici su interfaccia
            suspend fun removeStaticAddressesOnInterface() {
                val addresses = decodeList(api.getIpAddresses(), RouterOsOperation.IP_ADDRESSES)
                addresses.filter { it.iface == iface }.forEach { entry ->
                    entry.id?.let {
                        decodeUnit(api.removeIpAddress(NumbersRequest(it)), RouterOsOperation.IP_ADDRESS_REMOVE)
                    }
                }
            }

            if (effective.networkMode == NetworkMode.DHCP) {
                // DHCP: verifica se gia configurato e bound, altrimenti configura
                val existingDhcp = decodeList(api.getDhcpClientStatus(iface), RouterOsOperation.DHCP_CLIENT_STATUS).firstOrNull()

                // Se il client DHCP esiste ed e gia bound, non fare nulla
                if (existingDhcp != null &&
                    existingDhcp.disabled == "false" &&
                    existingDhcp.status?.equals("bound", ignoreCase = true) == true) {
                    // DHCP gia configurato correttamente, ritorna lo stato attuale
                    return@executeWithOutcome NetworkConfigFeedback(
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
                        // Client esiste ma e disabilitato o non bound: riabilita
                        if (existingDhcp.disabled == "true") {
                            decodeUnit(api.enableDhcpClient(NumbersRequest(dhcpId)), RouterOsOperation.DHCP_CLIENT_ENABLE)
                        } else {
                            // Client abilitato ma non bound: disable/enable per refresh
                            decodeUnit(api.disableDhcpClient(NumbersRequest(dhcpId)), RouterOsOperation.DHCP_CLIENT_DISABLE)
                            delay(500)
                            decodeUnit(api.enableDhcpClient(NumbersRequest(dhcpId)), RouterOsOperation.DHCP_CLIENT_ENABLE)
                        }
                    }
                } else {
                    // Client non esiste: crea
                    try {
                        decodeUnit(api.addDhcpClient(DhcpClientAdd(`interface` = iface)), RouterOsOperation.DHCP_CLIENT_ADD)
                    } catch (e: TestExecutionException) {
                        // Se il client esiste gia (race condition), recuperalo e abilitalo
                        val routerOsError = e.error as? TestError.RouterOsError
                        if (routerOsError?.category == RouterOsErrorCategory.ALREADY_EXISTS) {
                            delay(500)
                            val existingId = decodeList(api.getDhcpClientStatus(iface), RouterOsOperation.DHCP_CLIENT_STATUS).firstOrNull()?.id
                            if (existingId != null) {
                                decodeUnit(api.enableDhcpClient(NumbersRequest(existingId)), RouterOsOperation.DHCP_CLIENT_ENABLE)
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
                    val cur = decodeList(api.getDhcpClientStatus(iface), RouterOsOperation.DHCP_CLIENT_STATUS).firstOrNull()
                    if (cur?.status?.equals("bound", true) == true) {
                        lease = cur
                        return@repeat
                    }
                    delay(1000)
                }
                val bound = lease ?: decodeList(api.getDhcpClientStatus(iface), RouterOsOperation.DHCP_CLIENT_STATUS).firstOrNull()
                NetworkConfigFeedback(
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
                val dhcpId = decodeList(api.getDhcpClientStatus(iface), RouterOsOperation.DHCP_CLIENT_STATUS).firstOrNull()?.id
                if (dhcpId != null) {
                    decodeUnit(api.disableDhcpClient(NumbersRequest(dhcpId)), RouterOsOperation.DHCP_CLIENT_DISABLE)
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

                val gw = effective.staticGateway
                    ?: error(context.getString(R.string.error_static_gateway_missing))

                validateStaticInput(cidr, gw)

                // Le chiamate di scrittura non restituiscono body significativi; il decoder verifica l'esito HTTP.
                decodeUnit(api.addIpAddress(IpAddressAdd(address = cidr, `interface` = iface)), RouterOsOperation.IP_ADDRESS_ADD)
                decodeUnit(api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gw, comment = "MikLink_Auto")), RouterOsOperation.ROUTE_ADD)

                NetworkConfigFeedback(
                    mode = "STATIC",
                    interfaceName = iface,
                    address = cidr,
                    gateway = gw,
                    dns = null,
                    message = context.getString(R.string.status_static_configured)
                )
            }
        }
        return outcome.getOrThrow(callExecutor)
    }

    private fun validateStaticInput(cidr: String, gateway: String) {
        require(isValidCidr(cidr)) {
            "Invalid CIDR: $cidr. Use ip/prefix (e.g., 192.168.0.100/24) or a valid netmask."
        }
        require(isValidIpv4(gateway)) {
            "Invalid gateway IPv4 address: $gateway"
        }
    }

    private fun isValidCidr(value: String): Boolean {
        val parts = value.split("/")
        if (parts.size != 2) return false
        val ipPart = parts[0]
        if (!isValidIpv4(ipPart)) return false
        val suffix = parts[1]
        val prefixLength = suffix.toIntOrNull()
        if (prefixLength != null) {
            return prefixLength in 0..32
        }
        return isValidSubnetMask(suffix)
    }

    private fun isValidIpv4(address: String): Boolean {
        val parts = address.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull() ?: return false
            num in 0..255
        }
    }

    private fun isValidSubnetMask(mask: String): Boolean {
        val parts = mask.split(".")
        if (parts.size != 4) return false
        var value = 0
        for (part in parts) {
            val num = part.toIntOrNull() ?: return false
            if (num !in 0..255) return false
            value = (value shl 8) or num
        }
        if (value == 0) return false
        val inverted = value.inv()
        return (inverted + 1) and inverted == 0
    }

    private fun <T> CallOutcome<T>.getOrThrow(executor: MikroTikCallExecutor): T {
        return when (this) {
            is CallOutcome.Success -> value
            is CallOutcome.Failure -> {
                val primary = failures.firstOrNull()?.throwable
                    ?: IllegalStateException("Unknown call failure")
                failures.drop(1).forEach { primary.addSuppressed(it.throwable) }
                throw TestExecutionException(executor.classify(primary))
            }
        }
    }

    private fun <T> decodeList(response: retrofit2.Response<List<T>>, operation: RouterOsOperation): List<T> {
        return when (val decoded = decoder.decode(operation, response)) {
            is DecodedResult.Error -> throw TestExecutionException(decoded.error)
            is DecodedResult.Success -> decoded.value
        }
    }

    private fun decodeUnit(response: retrofit2.Response<Any>, operation: RouterOsOperation) {
        if (!response.isSuccessful) {
            when (val decoded = decoder.decode<Any>(operation, response)) {
                is DecodedResult.Error -> throw TestExecutionException(decoded.error)
                is DecodedResult.Success -> Unit
            }
        }
    }
}

/*
 * Purpose: Define MikroTik REST DTOs used by the data layer for requests and responses.
 * Inputs: Serialized JSON payloads from RouterOS REST endpoints.
 * Outputs: Typed request/response models consumed by repositories and mappers.
 * Notes: Keep field annotations aligned with RouterOS expectations (e.g., `.proplist`).
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// General request wrappers
data class ProplistRequest(
    @param:Json(name = ".proplist")
    val proplist: List<String>
)
data class InterfaceNameRequest(@param:Json(name = "?interface") val interfaceName: String)
data class NumbersRequest(@param:Json(name = "numbers") val numbers: String)

// System / Interface responses
@JsonClass(generateAdapter = true)
data class SystemResource(@param:Json(name = "board-name") val boardName: String? = null)
data class EthernetInterface(@param:Json(name = "name") val name: String)

// DHCP client
data class DhcpClientStatus(
    @param:Json(name = ".id") val id: String? = null,
    val disabled: String? = null,
    val status: String? = null,
    val address: String? = null,
    val gateway: String? = null,
    val dns: String? = null
)

// IP address management
data class IpAddressEntry(
    @param:Json(name = ".id") val id: String? = null,
    val address: String? = null,
    @param:Json(name = "interface") val iface: String? = null
)

data class IpAddressAdd(
    @param:Json(name = "address") val address: String,
    @param:Json(name = "interface") val `interface`: String
)

// Routes
data class RouteEntry(
    @param:Json(name = ".id") val id: String? = null,
    @param:Json(name = "dst-address") val dstAddress: String? = null,
    val gateway: String? = null,
    val comment: String? = null
)

data class RouteAdd(
    @param:Json(name = "dst-address") val dstAddress: String,
    val gateway: String,
    val comment: String? = null
)

// DHCP client add
data class DhcpClientAdd(
    @param:Json(name = "interface") val `interface`: String,
    @param:Json(name = "use-peer-dns") val usePeerDns: String = "yes"
)

// Cable test
data class CableTestRequest(
    @param:Json(name = "numbers") val numbers: String,
    val duration: String = "5s"
)

data class CableTestResult(
    @param:Json(name = "cable-pairs") val cablePairs: List<Map<String, String>>?,
    val status: String
)

// Link monitor
data class MonitorRequest(
    @param:Json(name = "numbers") val numbers: String,
    @param:Json(name = "once") val once: Boolean = true
)
data class MonitorResponse(val status: String, val rate: String?)

// Neighbor / LLDP
data class NeighborRequest(
    @param:Json(name = "?.query") val query: List<String>,
    @param:Json(name = ".proplist") val proplist: List<String>
)

data class NeighborDetail(
    val identity: String?,
    @param:Json(name = "interface-name") val interfaceName: String?,
    @param:Json(name = "system-caps-enabled") val systemCaps: String?,
    @param:Json(name = "discovered-by") val discoveredBy: String?,
    @param:Json(name = "vlan-id") val vlanId: String? = null,
    @param:Json(name = "voice-vlan-id") val voiceVlanId: String? = null,
    @param:Json(name = "poe-class") val poeClass: String? = null,
    @param:Json(name = "system-description") val systemDescription: String? = null,
    @param:Json(name = "port-id") val portId: String? = null
)

// Ping
data class PingRequest(
    val address: String,
    val `interface`: String? = null,
    val count: String = "4"
)

data class PingResult(
    @param:Json(name = "avg-rtt") val avgRtt: String?,
    val host: String?,
    @param:Json(name = "max-rtt") val maxRtt: String?,
    @param:Json(name = "min-rtt") val minRtt: String?,
    @param:Json(name = "packet-loss") val packetLoss: String?,
    val received: String?,
    val sent: String?,
    val seq: String?,
    val size: String?,
    val time: String?,
    val ttl: String?
)

// Note: SpeedTestRequest/Result live in their own files under this package and should not be duplicated.

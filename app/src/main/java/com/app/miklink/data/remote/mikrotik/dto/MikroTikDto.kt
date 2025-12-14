package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Json

// General request wrappers
data class ProplistRequest(@Json(name = ".proplist") val proplist: List<String>)
data class InterfaceNameRequest(@Json(name = "?interface") val interfaceName: String)
data class NumbersRequest(@Json(name = "numbers") val numbers: String)

// System / Interface responses
data class SystemResource(@Json(name = "board-name") val boardName: String)
data class EthernetInterface(@Json(name = "name") val name: String)

// DHCP client
data class DhcpClientStatus(
    @Json(name = ".id") val id: String? = null,
    val disabled: String? = null,
    val status: String? = null,
    val address: String? = null,
    val gateway: String? = null,
    val dns: String? = null
)

// IP address management
data class IpAddressEntry(
    @Json(name = ".id") val id: String? = null,
    val address: String? = null,
    @Json(name = "interface") val iface: String? = null
)

data class IpAddressAdd(@Json(name = "address") val address: String, @Json(name = "interface") val `interface`: String)

// Routes
data class RouteEntry(
    @Json(name = ".id") val id: String? = null,
    @Json(name = "dst-address") val dstAddress: String? = null,
    val gateway: String? = null
    ,val comment: String? = null
)

data class RouteAdd(
    @Json(name = "dst-address") val dstAddress: String,
    val gateway: String
    ,val comment: String? = null
)

// DHCP client add
data class DhcpClientAdd(@Json(name = "interface") val `interface`: String, @Json(name = "use-peer-dns") val usePeerDns: String = "yes")

// Cable test
data class CableTestRequest(
    @Json(name = "numbers") val numbers: String,
    val duration: String = "5s"
)

data class CableTestResult(
    @Json(name = "cable-pairs") val cablePairs: List<Map<String, String>>?,
    val status: String
)

// Link monitor
data class MonitorRequest(@Json(name = "numbers") val numbers: String, @Json(name = "once") val once: Boolean = true)
data class MonitorResponse(val status: String, val rate: String?)

// Neighbor / LLDP
data class NeighborRequest(@Json(name = "?.query") val query: List<String>, @Json(name = ".proplist") val proplist: List<String>)

data class NeighborDetail(
    val identity: String?,
    @Json(name = "interface-name") val interfaceName: String?,
    @Json(name = "system-caps-enabled") val systemCaps: String?,
    @Json(name = "discovered-by") val discoveredBy: String?,
    @Json(name = "vlan-id") val vlanId: String? = null,
    @Json(name = "voice-vlan-id") val voiceVlanId: String? = null,
    @Json(name = "poe-class") val poeClass: String? = null,
    @Json(name = "system-description") val systemDescription: String? = null,
    @Json(name = "port-id") val portId: String? = null
)

// Ping
data class PingRequest(
    val address: String,
    val `interface`: String? = null,
    val count: String = "4"
)

data class PingResult(
    @Json(name = "avg-rtt") val avgRtt: String?,
    val host: String?,
    @Json(name = "max-rtt") val maxRtt: String?,
    @Json(name = "min-rtt") val minRtt: String?,
    @Json(name = "packet-loss") val packetLoss: String?,
    val received: String?,
    val sent: String?,
    val seq: String?,
    val size: String?,
    val time: String?,
    val ttl: String?
)

// Note: SpeedTestRequest/Result live in their own files under this package and should not be duplicated.

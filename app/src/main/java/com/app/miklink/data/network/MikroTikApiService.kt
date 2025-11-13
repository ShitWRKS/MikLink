package com.app.miklink.data.network

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.POST

// --- DTOs (Data Transfer Objects) ---

// Richieste Generiche
data class ProplistRequest(@Json(name = ".proplist") val proplist: List<String>)
data class IdRequest(@Json(name = ".id") val id: String)
data class RemoveRequest(@Json(name = "numbers") val numbers: String)
data class InterfaceNameRequest(@Json(name = "?.interface") val interfaceName: String)


// Risorse Sonda
data class SystemResource(@Json(name = "board-name") val boardName: String)
data class EthernetInterface(val name: String)
data class DhcpClientStatus(val gateway: String?)

// Risultati Test
data class CableTestRequest(@Json(name = "numbers") val numbers: String)
data class CableTestResult(@Json(name = "cable-pairs") val cablePairs: List<Map<String, String>>, val status: String)

data class MonitorRequest(@Json(name = "numbers") val numbers: String, @Json(name = "once") val once: Boolean = true)
data class MonitorResponse(val status: String, val rate: String?)

data class NeighborRequest(@Json(name = "?.query") val query: List<String>, @Json(name = ".proplist") val proplist: List<String>)
data class NeighborDetail(
    val identity: String?,
    @Json(name = "interface-name") val interfaceName: String?,
    @Json(name = "system-caps-enabled") val systemCaps: String?,
    @Json(name = "discovered-by") val discoveredBy: String?,
    @Json(name = "vlan-id") val vlanId: String? = null,
    @Json(name = "voice-vlan-id") val voiceVlanId: String? = null,
    @Json(name = "poe-class") val poeClass: String? = null
)

data class PingRequest(val address: String, val count: String = "4")
data class PingResult(@Json(name = "avg-rtt") val avgRtt: String?)

data class TracerouteRequest(val address: String)
data class TracerouteResult(val address: String, val loss: String?)

// Configurazione Rete
data class VlanRequest(val name: String, @Json(name = "vlan-id") val vlanId: String, @Json(name = "interface") val interfaceName: String)
data class IpAddressRequest(val address: String, @Json(name = "interface") val interfaceName: String)


interface MikroTikApiService {

    @POST("/rest/system/resource/print")
    suspend fun getSystemResource(@Body request: ProplistRequest): List<SystemResource>

    @POST("/rest/interface/ethernet/print")
    suspend fun getEthernetInterfaces(@Body request: ProplistRequest): List<EthernetInterface>

    @POST("/rest/ip/dhcp-client/print")
    suspend fun getDhcpClientStatus(@Body request: InterfaceNameRequest): List<DhcpClientStatus>

    @POST("/rest/interface/ethernet/cable-test")
    suspend fun runCableTest(@Body request: CableTestRequest): List<CableTestResult>

    @POST("/rest/interface/ethernet/monitor")
    suspend fun getLinkStatus(@Body request: MonitorRequest): List<MonitorResponse>

    @POST("/rest/ip/neighbor/print")
    suspend fun getIpNeighbors(@Body request: NeighborRequest): List<NeighborDetail>

    @POST("/rest/ping")
    suspend fun runPing(@Body request: PingRequest): List<PingResult>

    @POST("/rest/tool/traceroute")
    suspend fun runTraceroute(@Body request: TracerouteRequest): List<TracerouteResult>

    @POST("/rest/interface/vlan/add")
    suspend fun addVlan(@Body request: VlanRequest): List<Map<String, String>>

    @POST("/rest/ip/address/add")
    suspend fun addIpAddress(@Body request: IpAddressRequest): List<Map<String, String>>

    @POST("/rest/interface/vlan/remove")
    suspend fun removeVlan(@Body request: RemoveRequest)

    @POST("/rest/ip/address/remove")
    suspend fun removeIpAddress(@Body request: RemoveRequest)
}
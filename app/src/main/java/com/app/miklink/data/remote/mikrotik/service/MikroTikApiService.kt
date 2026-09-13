package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.data.remote.mikrotik.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface MikroTikApiService {

    @POST("/rest/system/resource/print")
    suspend fun getSystemResource(@Body request: ProplistRequest = ProplistRequest(listOf("board-name"))): Response<List<SystemResource>>

    @GET("/rest/interface/ethernet")
    suspend fun getEthernetInterfaces(@Query(".proplist") proplist: String = "name"): Response<List<EthernetInterface>>

    // DHCP client management
    @GET("/rest/ip/dhcp-client")
    suspend fun getDhcpClientStatus(@Query("interface") interfaceName: String): Response<List<DhcpClientStatus>>

    @POST("/rest/ip/dhcp-client/add")
    suspend fun addDhcpClient(@Body request: DhcpClientAdd): Response<Any>

    @POST("/rest/ip/dhcp-client/enable")
    suspend fun enableDhcpClient(@Body request: NumbersRequest): Response<Any>

    @POST("/rest/ip/dhcp-client/disable")
    suspend fun disableDhcpClient(@Body request: NumbersRequest): Response<Any>

    // IP address management
    @GET("/rest/ip/address")
    suspend fun getIpAddresses(@Query(".proplist") proplist: String = ".id,address,interface"): Response<List<IpAddressEntry>>

    @POST("/rest/ip/address/add")
    suspend fun addIpAddress(@Body request: IpAddressAdd): Response<Any>

    @POST("/rest/ip/address/remove")
    suspend fun removeIpAddress(@Body request: NumbersRequest): Response<Any>

    // Routes management
    @GET("/rest/ip/route")
    suspend fun getRoutes(@Query(".proplist") proplist: String = ".id,dst-address,gateway"): Response<List<RouteEntry>>

    @POST("/rest/ip/route/add")
    suspend fun addRoute(@Body request: RouteAdd): Response<Any>

    @POST("/rest/ip/route/remove")
    suspend fun removeRoute(@Body request: NumbersRequest): Response<Any>

    // Tests
    @POST("/rest/interface/ethernet/cable-test")
    suspend fun runCableTest(@Body request: CableTestRequest): Response<List<CableTestResult>>

    @POST("/rest/interface/ethernet/monitor")
    suspend fun getLinkStatus(@Body request: MonitorRequest): Response<List<MonitorResponse>>

    @GET("/rest/ip/neighbor")
    suspend fun getIpNeighbors(
        @Query("interface") interfaceName: String,
        @Query(".proplist") proplist: String = "identity,interface-name,system-caps-enabled,discovered-by,vlan-id,voice-vlan-id,poe-class,system-description,port-id"
    ): Response<List<NeighborDetail>>

    @POST("/rest/ping")
    suspend fun runPing(@Body request: PingRequest): Response<List<PingResult>>

    @POST("/rest/tool/speed-test")
    suspend fun runSpeedTest(
        @Body request: SpeedTestRequest
    ): Response<List<SpeedTestResult>>
}

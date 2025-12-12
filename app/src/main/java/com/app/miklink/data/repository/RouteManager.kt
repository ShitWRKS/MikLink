package com.app.miklink.data.repository

import com.app.miklink.data.network.MikroTikApiService
import com.app.miklink.data.network.dto.RouteAdd
import com.app.miklink.data.network.dto.RouteEntry
import com.app.miklink.data.network.dto.NumbersRequest

/**
 * Abstraction for route management operations on MikroTik devices.
 */
interface RouteManager {
    suspend fun removeDefaultRoutes(api: MikroTikApiService, expectedGateway: String? = null, dryRun: Boolean = false)
    suspend fun addDefaultRoute(api: MikroTikApiService, gateway: String)
    suspend fun listRoutes(api: MikroTikApiService): List<RouteEntry>
}

@javax.inject.Singleton
class RouteManagerImpl @javax.inject.Inject constructor() : RouteManager {
    override suspend fun listRoutes(api: MikroTikApiService): List<RouteEntry> = api.getRoutes()

    override suspend fun addDefaultRoute(api: MikroTikApiService, gateway: String) {
        api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gateway, comment = "MikLink_Auto"))
    }

    override suspend fun removeDefaultRoutes(api: MikroTikApiService, expectedGateway: String?, dryRun: Boolean) {
        val routes = api.getRoutes()
        val candidates = routes.filter { r ->
            r.dstAddress == "0.0.0.0/0" && (
                r.comment == "MikLink_Auto" || (expectedGateway != null && r.gateway == expectedGateway)
            )
        }

        if (dryRun) {
            android.util.Log.d("RouteManager", "Dry-run removeDefaultRoutes: candidates = ${candidates.map { it.id }}")
            return
        }

        val removedRoutes = mutableListOf<RouteEntry>()
        try {
            candidates.forEach { r ->
                r.id?.let {
                    api.removeRoute(NumbersRequest(it))
                    removedRoutes.add(r)
                }
            }
        } catch (e: Exception) {
            // rollback
            android.util.Log.e("RouteManager", "removeDefaultRoutes failed - rolling back", e)
            removedRoutes.forEach { r ->
                try {
                    api.addRoute(RouteAdd(dstAddress = r.dstAddress ?: "0.0.0.0/0", gateway = r.gateway ?: "", comment = r.comment))
                } catch (re: Exception) {
                    android.util.Log.e("RouteManager", "Rollback failed for route ${r.id}", re)
                }
            }
            throw e
        }
    }
}

package com.app.miklink.data.repository

import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.dto.RouteAdd
import com.app.miklink.data.remote.mikrotik.dto.RouteEntry
import com.app.miklink.data.remote.mikrotik.dto.NumbersRequest
import com.app.miklink.core.domain.test.model.TestExecutionException
import com.app.miklink.data.remote.mikrotik.service.DecodedResult
import com.app.miklink.data.remote.mikrotik.service.RouterOsOperation
import com.app.miklink.data.remote.mikrotik.service.RouterOsResponseDecoder
import retrofit2.Response

/**
 * Abstraction for route management operations on MikroTik devices.
 */
interface RouteManager {
    suspend fun removeDefaultRoutes(api: MikroTikApiService, expectedGateway: String? = null, dryRun: Boolean = false)
    suspend fun addDefaultRoute(api: MikroTikApiService, gateway: String)
    suspend fun listRoutes(api: MikroTikApiService): List<RouteEntry>
}

@javax.inject.Singleton
class RouteManagerImpl @javax.inject.Inject constructor(
    private val decoder: RouterOsResponseDecoder
) : RouteManager {
    override suspend fun listRoutes(api: MikroTikApiService): List<RouteEntry> =
        decodeList(api.getRoutes(), RouterOsOperation.ROUTES)

    override suspend fun addDefaultRoute(api: MikroTikApiService, gateway: String) {
        decodeUnit(
            api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gateway, comment = "MikLink_Auto")),
            RouterOsOperation.ROUTE_ADD
        )
    }

    override suspend fun removeDefaultRoutes(api: MikroTikApiService, expectedGateway: String?, dryRun: Boolean) {
        val routes = decodeList(api.getRoutes(), RouterOsOperation.ROUTES)
        val candidates = routes.filter { r ->
            r.dstAddress == "0.0.0.0/0" && (
                r.comment == "MikLink_Auto" || (expectedGateway != null && r.gateway == expectedGateway)
            )
        }

        if (dryRun) {
            if (com.app.miklink.BuildConfig.DEBUG) android.util.Log.d("RouteManager", "Dry-run removeDefaultRoutes: candidates = ${candidates.map { it.id }}")
            return
        }

        val removedRoutes = mutableListOf<RouteEntry>()
        try {
            candidates.forEach { r ->
                r.id?.let {
                    decodeUnit(api.removeRoute(NumbersRequest(it)), RouterOsOperation.ROUTE_REMOVE)
                    removedRoutes.add(r)
                }
            }
        } catch (e: Exception) {
            // rollback
            if (com.app.miklink.BuildConfig.DEBUG) android.util.Log.e("RouteManager", "removeDefaultRoutes failed - rolling back", e)
            removedRoutes.asReversed().forEach { r ->
                try {
                    decodeUnit(
                        api.addRoute(
                            RouteAdd(
                                dstAddress = r.dstAddress ?: "0.0.0.0/0",
                                gateway = r.gateway ?: "",
                                comment = r.comment
                            )
                        ),
                        RouterOsOperation.ROUTE_ADD
                    )
                } catch (re: Exception) {
                    if (com.app.miklink.BuildConfig.DEBUG) android.util.Log.e("RouteManager", "Rollback failed for route ${r.id}", re)
                    e.addSuppressed(re)
                }
            }
            throw e
        }
    }

    private fun <T> decodeList(response: Response<List<T>>, operation: RouterOsOperation): List<T> =
        when (val decoded = decoder.decode(operation, response)) {
            is DecodedResult.Success -> decoded.value
            is DecodedResult.Error -> throw TestExecutionException(decoded.error)
        }

    private fun decodeUnit(response: Response<Any>, operation: RouterOsOperation) {
        if (response.isSuccessful) return
        when (val decoded = decoder.decode(operation, response)) {
            is DecodedResult.Success -> Unit
            is DecodedResult.Error -> throw TestExecutionException(decoded.error)
        }
    }
}

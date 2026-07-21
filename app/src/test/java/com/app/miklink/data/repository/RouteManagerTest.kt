package com.app.miklink.data.repository

import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionException
import com.app.miklink.data.remote.mikrotik.dto.NumbersRequest
import com.app.miklink.data.remote.mikrotik.dto.RouteAdd
import com.app.miklink.data.remote.mikrotik.dto.RouteEntry
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.data.remote.mikrotik.service.RouterOsResponseDecoder
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class RouteManagerTest {
    private val api: MikroTikApiService = mockk()
    private val manager = RouteManagerImpl(RouterOsResponseDecoder(Moshi.Builder().build()))

    @Before
    fun mockAndroidLog() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @Test
    fun `getRoutes HTTP 401 fails with authentication`() = runBlocking {
        coEvery { api.getRoutes(any()) } returns errorResponse(401)

        val failure = runCatching { manager.listRoutes(api) }.exceptionOrNull() as TestExecutionException

        assertTrue(failure.error is TestError.Authentication)
    }

    @Test
    fun `getRoutes HTTP 500 fails with RouterOS error`() = runBlocking {
        coEvery { api.getRoutes(any()) } returns errorResponse(500)

        val failure = runCatching { manager.listRoutes(api) }.exceptionOrNull() as TestExecutionException

        assertEquals(500, (failure.error as TestError.RouterOsError).code)
    }

    @Test
    fun `addRoute HTTP 400 fails`() = runBlocking {
        coEvery { api.addRoute(any()) } returns errorResponse(400)

        val failure = runCatching { manager.addDefaultRoute(api, "192.168.1.1") }
            .exceptionOrNull() as TestExecutionException

        assertEquals(400, (failure.error as TestError.RouterOsError).code)
    }

    @Test
    fun `removeRoute HTTP 500 fails`() = runBlocking {
        val route = managedRoute("*1", "192.168.1.1")
        coEvery { api.getRoutes(any()) } returns Response.success(listOf(route))
        coEvery { api.removeRoute(NumbersRequest("*1")) } returns errorResponse(500)

        val failure = runCatching { manager.removeDefaultRoutes(api, null) }
            .exceptionOrNull() as TestExecutionException

        assertEquals(500, (failure.error as TestError.RouterOsError).code)
        coVerify(exactly = 0) { api.addRoute(any()) }
    }

    @Test
    fun `remove failure rolls back routes already removed`() = runBlocking {
        val first = managedRoute("*1", "192.168.1.1")
        val second = managedRoute("*2", "192.168.1.2")
        coEvery { api.getRoutes(any()) } returns Response.success(listOf(first, second))
        coEvery { api.removeRoute(NumbersRequest("*1")) } returns Response.success(null)
        coEvery { api.removeRoute(NumbersRequest("*2")) } returns errorResponse(500)
        coEvery { api.addRoute(any()) } returns Response.success(null)

        val failure = runCatching { manager.removeDefaultRoutes(api, null) }.exceptionOrNull()

        assertTrue(failure is TestExecutionException)
        assertTrue(failure?.suppressed?.isEmpty() == true)
        coVerify(exactly = 1) {
            api.addRoute(RouteAdd("0.0.0.0/0", "192.168.1.1", "MikLink_Auto"))
        }
    }

    @Test
    fun `rollback HTTP failure is suppressed on primary remove failure`() = runBlocking {
        val first = managedRoute("*1", "192.168.1.1")
        val second = managedRoute("*2", "192.168.1.2")
        coEvery { api.getRoutes(any()) } returns Response.success(listOf(first, second))
        coEvery { api.removeRoute(NumbersRequest("*1")) } returns Response.success(null)
        coEvery { api.removeRoute(NumbersRequest("*2")) } returns errorResponse(500)
        coEvery { api.addRoute(any()) } returns errorResponse(400)

        val failure = runCatching { manager.removeDefaultRoutes(api, null) }
            .exceptionOrNull() as TestExecutionException

        assertEquals(500, (failure.error as TestError.RouterOsError).code)
        assertEquals(1, failure.suppressed.size)
        val rollbackFailure = failure.suppressed.single() as TestExecutionException
        assertEquals(400, (rollbackFailure.error as TestError.RouterOsError).code)
    }

    private fun managedRoute(id: String, gateway: String) = RouteEntry(
        id = id,
        dstAddress = "0.0.0.0/0",
        gateway = gateway,
        comment = "MikLink_Auto"
    )

    private fun <T> errorResponse(code: Int): Response<T> = Response.error(
        code,
        """{"error":$code,"message":"request failed","detail":"failure"}"""
            .toResponseBody("application/json".toMediaType())
    )
}

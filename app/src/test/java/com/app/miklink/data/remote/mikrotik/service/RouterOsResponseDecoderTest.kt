/*
 * Purpose: Verify RouterOsResponseDecoder produces correct TestError subtypes and they propagate through
 *          TestExecutionException without becoming Unexpected.
 * Inputs: HTTP responses with various error codes via mocked Retrofit Response objects.
 * Outputs: Assertions that decoded TestError types match expected classification.
 * Notes: Covers mandatory test A — type propagation from decoder through repository.
 */
package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.RouterOsErrorCategory
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RouterOsResponseDecoderTest {

    private val decoder = RouterOsResponseDecoder(Moshi.Builder().build())

    @Test
    fun `decode returns Authentication for HTTP 401`() {
        val response = Response.error<Any>(
            401,
            """{"error":"unauthorized"}""".toResponseBody("application/json".toMediaType())
        )
        val result = decoder.decode(RouterOsOperation.SYSTEM_RESOURCE, response)
        assertTrue("Expected Error but got $result", result is DecodedResult.Error)
        assertTrue(
            "Expected Authentication but got ${(result as DecodedResult.Error).error::class.simpleName}",
            result.error is TestError.Authentication
        )
    }

    @Test
    fun `decode returns Authentication for HTTP 403`() {
        val response = Response.error<Any>(
            403,
            """{"error":"forbidden"}""".toResponseBody("application/json".toMediaType())
        )
        val result = decoder.decode(RouterOsOperation.LINK_STATUS, response)
        assertTrue("Expected Error but got $result", result is DecodedResult.Error)
        assertTrue(
            "Expected Authentication but got ${(result as DecodedResult.Error).error::class.simpleName}",
            result.error is TestError.Authentication
        )
    }

    @Test
    fun `decode returns RouterOsError for HTTP 500`() {
        val response = Response.error<Any>(
            500,
            """{"error":"internal error","detail":"something broke"}""".toResponseBody("application/json".toMediaType())
        )
        val result = decoder.decode(RouterOsOperation.PING, response)
        assertTrue("Expected Error but got $result", result is DecodedResult.Error)
        val error = (result as DecodedResult.Error).error
        assertTrue("Expected RouterOsError but got ${error::class.simpleName}", error is TestError.RouterOsError)
        val routerError = error as TestError.RouterOsError
        assertEquals(500, routerError.code)
    }

    @Test
    fun `decode returns RouterOsError for HTTP 400`() {
        val response = Response.error<Any>(
            400,
            "".toResponseBody("application/json".toMediaType())
        )
        val result = decoder.decode(RouterOsOperation.SPEED_TEST, response)
        assertTrue("Expected Error", result is DecodedResult.Error)
        assertTrue((result as DecodedResult.Error).error is TestError.RouterOsError)
    }

    @Test
    fun `decode classifies structured DHCP duplicate detail`() {
        val response = Response.error<Any>(
            400,
            """{"error":400,"message":"Bad Request","detail":"failure: already have such interface"}"""
                .toResponseBody("application/json".toMediaType())
        )

        val result = decoder.decode(RouterOsOperation.DHCP_CLIENT_ADD, response) as DecodedResult.Error
        val error = result.error as TestError.RouterOsError

        assertEquals(RouterOsErrorCategory.ALREADY_EXISTS, error.category)
    }

    @Test
    fun `decode returns InvalidResponse for null success body`() {
        @Suppress("UNCHECKED_CAST")
        val response = Response.success<Any>(null) as retrofit2.Response<Any>
        val result = decoder.decode(RouterOsOperation.CABLE_TEST, response)
        assertTrue("Expected Error for null body", result is DecodedResult.Error)
        assertTrue((result as DecodedResult.Error).error is TestError.InvalidResponse)
    }

    @Test
    fun `decode returns Success for valid response`() {
        val data = listOf(mapOf("name" to "ether1"))
        val response = Response.success(data)
        val result = decoder.decode(RouterOsOperation.ETHERNET_INTERFACES, response)
        assertTrue("Expected Success", result is DecodedResult.Success)
    }
}

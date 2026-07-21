/*
 * Purpose: Single decoder responsible for HTTP verification, success-body reading, error-body
 * decoding and classification into typed TestError categories.
 * Inputs: retrofit2.Response<T> plus the endpoint operation that produced it.
 * Outputs: DecodedResult.Success(value) or DecodedResult.Error(testError).
 * Notes: Does NOT apply user thresholds; does NOT interpret errors from free-form strings outside
 * this component. Textual fallbacks are allowed only when tied to a specific HTTP code + operation
 * and covered by a real fixture (never converts unknown errors to Unsupported).
 */
package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.RouterOsErrorCategory
import com.app.miklink.data.remote.mikrotik.dto.RouterOsErrorBody
import com.app.miklink.di.RouterOsMoshi
import com.squareup.moshi.Moshi
import okhttp3.ResponseBody
import java.io.IOException

/**
 * Operazione RouterOS che ha prodotto la risposta; usata per associare endpoint e classificazione.
 */
enum class RouterOsOperation {
    SYSTEM_RESOURCE,
    ETHERNET_INTERFACES,
    DHCP_CLIENT_STATUS,
    DHCP_CLIENT_ADD,
    DHCP_CLIENT_ENABLE,
    DHCP_CLIENT_DISABLE,
    IP_ADDRESSES,
    IP_ADDRESS_ADD,
    IP_ADDRESS_REMOVE,
    ROUTES,
    ROUTE_ADD,
    ROUTE_REMOVE,
    CABLE_TEST,
    LINK_STATUS,
    NEIGHBORS,
    PING,
    SPEED_TEST
}

sealed interface DecodedResult<out T> {
    data class Success<T>(val value: T) : DecodedResult<T>
    data class Error(val error: TestError) : DecodedResult<Nothing>
}

class RouterOsResponseDecoder @javax.inject.Inject constructor(
    @RouterOsMoshi private val moshi: Moshi
) {
    fun <T> decode(
        operation: RouterOsOperation,
        response: retrofit2.Response<T>
    ): DecodedResult<T> {
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null) {
                return DecodedResult.Error(
                    TestError.InvalidResponse("Empty success body for ${operation.name}")
                )
            }
            return DecodedResult.Success(body)
        }

        val code = response.code()
        val errorBody = response.errorBody()
        val decoded = decodeErrorBody(code, operation, errorBody)

        return when (code) {
            401, 403 -> DecodedResult.Error(
                TestError.Authentication(
                    message = decoded?.message ?: "Authentication failed (HTTP $code)",
                    cause = null
                )
            )
            else -> {
                val message = decoded?.message ?: fallbackMessage(code, operation)
                val detail = decoded?.detail
                DecodedResult.Error(
                TestError.RouterOsError(
                    message = message,
                    code = code,
                    detail = detail,
                    category = classifyCategory(operation, decoded)
                )
                )
            }
        }
    }

    private fun classifyCategory(
        operation: RouterOsOperation,
        body: RouterOsErrorBody?
    ): RouterOsErrorCategory? {
        if (operation != RouterOsOperation.DHCP_CLIENT_ADD || body == null) return null
        val structuredDetail = body.detail?.trim()?.lowercase()
        return if (structuredDetail in DHCP_ALREADY_EXISTS_DETAILS) {
            RouterOsErrorCategory.ALREADY_EXISTS
        } else {
            null
        }
    }

    private fun decodeErrorBody(
        code: Int,
        operation: RouterOsOperation,
        errorBody: ResponseBody?
    ): RouterOsErrorBody? {
        if (errorBody == null) return null
        return try {
            val adapter = moshi.adapter(RouterOsErrorBody::class.java)
            val raw = errorBody.string()
            if (raw.isBlank()) return null
            adapter.fromJson(raw)
        } catch (_: Exception) {
            // Malformed error body: do not crash; classification falls back to HTTP code.
            null
        }
    }

    private fun fallbackMessage(code: Int, operation: RouterOsOperation): String {
        // Textual fallback tied to a specific HTTP code + operation, covered by fixtures.
        return when {
            code == 400 && operation == RouterOsOperation.SPEED_TEST ->
                "Bad speed test request (HTTP 400)"
            code == 404 ->
                "Resource not found (HTTP 404) for ${operation.name}"
            code == 406 ->
                "Not acceptable (HTTP 406) for ${operation.name}"
            else ->
                "RouterOS error (HTTP $code) for ${operation.name}"
        }
    }
}

private val DHCP_ALREADY_EXISTS_DETAILS = setOf(
    "already exists",
    "failure: already have such interface"
)

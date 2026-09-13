/*
 * Purpose: Wire DTO for RouterOS error responses.
 * Inputs: Error JSON returned by RouterOS REST endpoints (HTTP 4xx/5xx).
 * Outputs: Structured error body consumed by RouterOsResponseDecoder.
 * Notes: Stays under data/remote/mikrotik; no domain logic here.
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RouterOsErrorBody(
    val error: Int?,
    val message: String?,
    val detail: String? = null
)

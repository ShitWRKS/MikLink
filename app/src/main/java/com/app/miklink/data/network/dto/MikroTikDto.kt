package com.app.miklink.data.network.dto

import com.squareup.moshi.Json

// DTO for /system/resource/print
data class SystemResourceResponse(
    @Json(name = "board-name") val boardName: String
)

// DTO for /interface/ethernet/print
data class EthernetInterfaceResponse(
    @Json(name = "name") val name: String
)

// Wrapper for the result of a probe connection check
sealed class ProbeCheckResult {
    data class Success(
        val boardName: String,
        val interfaces: List<String>,
        val tdrSupported: Boolean
    ) : ProbeCheckResult()

    data class Error(val message: String) : ProbeCheckResult()
}
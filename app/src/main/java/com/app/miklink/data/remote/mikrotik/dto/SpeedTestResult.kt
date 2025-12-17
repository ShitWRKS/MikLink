/*
 * Purpose: DTO for MikroTik speed test responses parsed from RouterOS HTTP API calls.
 * Inputs: Raw JSON payloads with ping, jitter, throughput, and warning fields.
 * Outputs: Structured results for repository/domain mapping.
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Json

data class SpeedTestResult(
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "ping-min-avg-max") val ping: String?,
    @param:Json(name = "jitter-min-avg-max") val jitter: String?,
    @param:Json(name = "loss") val loss: String?,
    @param:Json(name = "tcp-download") val tcpDownload: String?,
    @param:Json(name = "tcp-upload") val tcpUpload: String?,
    @param:Json(name = "udp-download") val udpDownload: String?,
    @param:Json(name = "udp-upload") val udpUpload: String?,
    @param:Json(name = ".about") val warning: String? // Messaggio CPU warning
)

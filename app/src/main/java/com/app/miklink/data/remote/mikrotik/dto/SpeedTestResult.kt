package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Json

data class SpeedTestResult(
    @Json(name = "status") val status: String?,
    @Json(name = "ping-min-avg-max") val ping: String?,
    @Json(name = "jitter-min-avg-max") val jitter: String?,
    @Json(name = "loss") val loss: String?,
    @Json(name = "tcp-download") val tcpDownload: String?,
    @Json(name = "tcp-upload") val tcpUpload: String?,
    @Json(name = "udp-download") val udpDownload: String?,
    @Json(name = "udp-upload") val udpUpload: String?,
    @Json(name = ".about") val warning: String? // Messaggio CPU warning
)

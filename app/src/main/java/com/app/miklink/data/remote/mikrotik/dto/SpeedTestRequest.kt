package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Json

data class SpeedTestRequest(
    @Json(name = "address") val address: String,
    @Json(name = "user") val user: String,
    @Json(name = "password") val password: String,
    @Json(name = "test-duration") val testDuration: String = "5"
)

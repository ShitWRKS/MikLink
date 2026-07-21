package com.app.miklink.core.domain.model

data class ProbeConfig(
    val ipAddress: String,
    val username: String,
    val password: String,
    val testInterface: String,
    val isHttps: Boolean,
    val isOnline: Boolean,
    val modelName: String?,
    val tdrCapability: TdrCapability
) {
    val shouldAttemptTdr: Boolean
        get() = tdrCapability != TdrCapability.UNSUPPORTED
}

package com.app.miklink.e2e.support

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TdrCapability

object PrerequisiteEvaluator {
    fun debugBuild(isDebug: Boolean): PrerequisiteResult = result(
        id = "debug-build",
        available = isDebug,
        unavailableReason = "AGENT_MODE_DEBUG_ONLY"
    )

    fun device(state: String?, serial: String?): PrerequisiteResult {
        val available = state == "device" && !serial.isNullOrBlank()
        return result("android-device", available, "DEVICE_${state?.uppercase() ?: "MISSING"}")
    }

    fun configuredProbe(config: ProbeConfig?): PrerequisiteResult = result(
        id = "configured-probe",
        available = config != null && config.ipAddress.isNotBlank() && config.username.isNotBlank(),
        unavailableReason = "PROBE_NOT_CONFIGURED"
    )

    fun reachableProbe(config: ProbeConfig?): PrerequisiteResult = result(
        id = "reachable-probe",
        available = config?.isOnline == true,
        unavailableReason = "PROBE_UNREACHABLE"
    )

    fun authenticatedProbe(authenticated: Boolean?): PrerequisiteResult = result(
        id = "probe-authentication",
        available = authenticated == true,
        unavailableReason = if (authenticated == false) "PROBE_AUTHENTICATION_FAILED" else "PROBE_AUTHENTICATION_NOT_EVALUATED"
    )

    fun tdrCapability(config: ProbeConfig?): PrerequisiteResult = when (config?.tdrCapability) {
        TdrCapability.UNSUPPORTED -> PrerequisiteResult(
            "tdr-capability",
            required = false,
            status = PrerequisiteStatus.NOT_APPLICABLE,
            reasonCode = "TDR_UNSUPPORTED"
        )
        null -> result("tdr-capability", false, "PROBE_NOT_CONFIGURED", required = false)
        else -> result("tdr-capability", true, "TDR_CAPABILITY_UNKNOWN", required = false)
    }

    fun speedServer(client: Client?): PrerequisiteResult = result(
        id = "speed-server",
        available = !client?.speedTestServerAddress.isNullOrBlank(),
        unavailableReason = "SPEED_SERVER_NOT_CONFIGURED"
    )

    fun wifiDisruption(policy: SessionPolicy): PrerequisiteResult = result(
        id = "wifi-disruption-authorized",
        available = policy.allowWifiDisruption && policy.hostControlRetained,
        unavailableReason = if (!policy.allowWifiDisruption) {
            "WIFI_DISRUPTION_NOT_AUTHORIZED"
        } else {
            "HOST_CONTROL_NOT_RETAINED"
        }
    )

    fun requireAvailable(results: List<PrerequisiteResult>): PrerequisiteResult? =
        results.firstOrNull { it.required && it.status == PrerequisiteStatus.UNAVAILABLE }

    private fun result(
        id: String,
        available: Boolean,
        unavailableReason: String,
        required: Boolean = true
    ) = PrerequisiteResult(
        id = id,
        required = required,
        status = if (available) PrerequisiteStatus.AVAILABLE else PrerequisiteStatus.UNAVAILABLE,
        reasonCode = if (available) "AVAILABLE" else unavailableReason
    )
}

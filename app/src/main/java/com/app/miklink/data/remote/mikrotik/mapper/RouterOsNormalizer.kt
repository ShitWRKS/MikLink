/*
 * Purpose: Centralized normalization of raw RouterOS DTO values into pure-Kotlin domain models.
 * Inputs: MikroTik REST DTOs (no Retrofit/Moshi annotations on outputs).
 * Outputs: Normalized domain models (LinkStatusData, CableTestSummary, PingMeasurement,
 *          NeighborData, SpeedTestData) with consistent units and missing-data semantics.
 * Notes: A non-interpretable value must NOT become 0; empty strings are treated as missing data.
 *        Report v1 is preserved via explicit conversion in a single point (see toReportData).
 */
package com.app.miklink.data.remote.mikrotik.mapper

import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.model.report.TdrEntry
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.data.remote.mikrotik.dto.CableTestResult
import com.app.miklink.data.remote.mikrotik.dto.MonitorResponse
import com.app.miklink.data.remote.mikrotik.dto.NeighborDetail
import com.app.miklink.data.remote.mikrotik.dto.PingResult
import com.app.miklink.data.remote.mikrotik.dto.SpeedTestResult

object RouterOsNormalizer {

    fun normalizeLinkStatus(response: MonitorResponse): LinkStatusData {
        val status = normalizeLinkStatusValue(response.status)
        val rate = normalizeRateMbps(response.rate)
        return LinkStatusData(status = status, rate = rate)
    }

    fun normalizeCableTest(result: CableTestResult): CableTestSummary {
        val entries = result.cablePairs.orEmpty().mapNotNull { pair ->
            val distance = pair["distance"] ?: pair["len"] ?: pair["length"]
            val statusValue = pair["status"] ?: pair["state"] ?: pair["result"]
            val description = pair["pair"] ?: pair["description"]
            if (distance == null && statusValue == null && description == null) {
                null
            } else {
                TdrEntry(
                    distance = distance?.trim()?.takeIf { it.isNullOrBlank().not() },
                    status = statusValue?.trim()?.takeIf { it.isNullOrBlank().not() },
                    description = description?.trim()?.takeIf { it.isNullOrBlank().not() }
                )
            }
        }.ifEmpty {
            listOf(TdrEntry(status = result.status?.trim()?.takeIf { it.isNotBlank() }))
        }
        return CableTestSummary(status = result.status?.trim()?.takeIf { it.isNotBlank() } ?: "unknown", entries = entries)
    }

    fun normalizePing(result: PingResult): PingMeasurement {
        return PingMeasurement(
            host = result.host?.trim()?.takeIf { it.isNotBlank() },
            minRtt = normalizeMilliseconds(result.minRtt),
            avgRtt = normalizeMilliseconds(result.avgRtt),
            maxRtt = normalizeMilliseconds(result.maxRtt),
            packetLoss = normalizePercent(result.packetLoss),
            sent = normalizeInt(result.sent),
            received = normalizeInt(result.received),
            seq = normalizeInt(result.seq),
            time = normalizeMilliseconds(result.time),
            ttl = normalizeInt(result.ttl),
            size = normalizeInt(result.size)
        )
    }

    fun normalizeNeighbor(detail: NeighborDetail): NeighborData {
        return NeighborData(
            identity = detail.identity?.trim()?.takeIf { it.isNotBlank() },
            interfaceName = detail.interfaceName?.trim()?.takeIf { it.isNotBlank() },
            discoveredBy = detail.discoveredBy?.trim()?.takeIf { it.isNotBlank() },
            vlanId = normalizeInt(detail.vlanId?.trim()),
            voiceVlanId = normalizeInt(detail.voiceVlanId?.trim()),
            poeClass = detail.poeClass?.trim()?.takeIf { it.isNotBlank() },
            systemDescription = detail.systemDescription?.trim()?.takeIf { it.isNotBlank() },
            portId = detail.portId?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    fun normalizeSpeedTest(result: SpeedTestResult, serverAddress: String?): SpeedTestData {
        return SpeedTestData(
            status = result.status?.trim()?.takeIf { it.isNotBlank() },
            ping = normalizeMilliseconds(result.ping),
            jitter = normalizeMilliseconds(result.jitter),
            loss = normalizePercent(result.loss),
            tcpDownload = normalizeThroughputMbps(result.tcpDownload),
            tcpUpload = normalizeThroughputMbps(result.tcpUpload),
            udpDownload = normalizeThroughputMbps(result.udpDownload),
            udpUpload = normalizeThroughputMbps(result.udpUpload),
            warning = result.warning?.trim()?.takeIf { it.isNotBlank() },
            serverAddress = serverAddress
        )
    }

    // --- Primitive normalization helpers ---

    fun normalizeLinkStatusValue(raw: String?): String {
        val value = raw?.trim()?.lowercase() ?: return "UNKNOWN"
        return when {
            value == "link-ok" || value == "up" || value == "running" -> "UP"
            value == "no-link" || value == "down" || value == "disabled" -> "DOWN"
            value.isBlank() -> "UNKNOWN"
            else -> "UNKNOWN"
        }
    }

    fun normalizeRateMbps(raw: String?): String? {
        val value = raw?.trim() ?: return null
        if (value.isBlank()) return null
        // RouterOS rates look like "1Gbps", "100Mbps", "10Mbps", "2.5Gbps"
        val lower = value.lowercase()
        return when {
            lower.endsWith("gbps") -> {
                val num = lower.removeSuffix("gbps").toDoubleOrNull() ?: return value
                "${num * 1000.0}Mbps"
            }
            lower.endsWith("mbps") -> {
                val num = lower.removeSuffix("mbps").toDoubleOrNull() ?: return value
                "${num}Mbps"
            }
            else -> value // non-interpretable: keep raw, do NOT coerce to 0
        }
    }

    fun normalizeMilliseconds(raw: String?): String? {
        val value = raw?.trim() ?: return null
        if (value.isBlank()) return null
        // RouterOS uses "ms" suffix; keep numeric part as milliseconds.
        val lower = value.lowercase()
        return when {
            lower.endsWith("ms") -> {
                val num = lower.removeSuffix("ms").toDoubleOrNull() ?: return value
                num.toString()
            }
            lower == "0" -> "0"
            else -> {
                // bare number assumed already in ms
                value.toDoubleOrNull()?.toString() ?: value
            }
        }
    }

    fun normalizePercent(raw: String?): String? {
        val value = raw?.trim() ?: return null
        if (value.isBlank()) return null
        val lower = value.lowercase()
        return when {
            lower.endsWith("%") -> {
                val num = lower.removeSuffix("%").toDoubleOrNull() ?: return value
                num.toString()
            }
            lower == "0" -> "0"
            else -> value.toDoubleOrNull()?.toString() ?: value
        }
    }

    fun normalizeThroughputMbps(raw: String?): String? {
        val value = raw?.trim() ?: return null
        if (value.isBlank()) return null
        val lower = value.lowercase()
        return when {
            lower.endsWith("gbps") -> {
                val num = lower.removeSuffix("gbps").toDoubleOrNull() ?: return value
                "${num * 1000.0}Mbps"
            }
            lower.endsWith("mbps") -> {
                val num = lower.removeSuffix("mbps").toDoubleOrNull() ?: return value
                "${num}Mbps"
            }
            lower == "0" -> "0"
            else -> value.toDoubleOrNull()?.let { "${it}Mbps" } ?: value
        }
    }

    fun normalizeInt(raw: String?): String? {
        val value = raw?.trim() ?: return null
        if (value.isBlank()) return null
        return value.toIntOrNull()?.toString() ?: value // non-interpretable kept as-is
    }
}

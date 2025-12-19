package com.app.miklink.core.domain.model.report

data class LinkStatusData(
    val status: String? = null,
    val rate: String? = null
)

data class TdrEntry(
    val distance: String? = null,
    val status: String? = null,
    val description: String? = null
)

data class NeighborData(
    val identity: String? = null,
    val interfaceName: String? = null,
    val discoveredBy: String? = null,
    val vlanId: String? = null,
    val voiceVlanId: String? = null,
    val poeClass: String? = null,
    val systemDescription: String? = null,
    val portId: String? = null
)

data class PingSample(
    val target: String? = null,
    val host: String? = null,
    val minRtt: String? = null,
    val avgRtt: String? = null,
    val maxRtt: String? = null,
    val packetLoss: String? = null,
    val sent: String? = null,
    val received: String? = null,
    val seq: String? = null,
    val time: String? = null,
    val ttl: String? = null,
    val size: String? = null,
    val error: String? = null
)

data class SpeedTestData(
    val status: String? = null,
    val ping: String? = null,
    val jitter: String? = null,
    val loss: String? = null,
    val tcpDownload: String? = null,
    val tcpUpload: String? = null,
    val udpDownload: String? = null,
    val udpUpload: String? = null,
    val warning: String? = null,
    val serverAddress: String? = null
)

data class NetworkData(
    val mode: String? = null,
    val address: String? = null,
    val gateway: String? = null,
    val dns: String? = null,
    val message: String? = null
)

data class ReportData(
    val network: NetworkData? = null,
    val linkStatus: LinkStatusData? = null,
    val tdr: List<TdrEntry> = emptyList(),
    val neighbors: List<NeighborData> = emptyList(),
    val pingSamples: List<PingSample> = emptyList(),
    val speedTest: SpeedTestData? = null,
    val extra: Map<String, String> = emptyMap()
)

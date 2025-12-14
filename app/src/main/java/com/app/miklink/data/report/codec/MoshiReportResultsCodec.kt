package com.app.miklink.data.report.codec

import com.app.miklink.core.data.report.ReportResultsCodec
import com.app.miklink.core.domain.model.report.ReportData
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import javax.inject.Inject

class MoshiReportResultsCodec @Inject constructor(
    moshi: Moshi
) : ReportResultsCodec {

    private val reportDataAdapter: JsonAdapter<ReportDataPayload> = moshi.adapter(ReportDataPayload::class.java)

    override fun decode(json: String): Result<ReportData> = runCatching {
        val payload = reportDataAdapter.fromJson(json)
            ?: throw IllegalArgumentException("Invalid report data payload")
        payload.toDomain()
    }

    override fun encode(data: ReportData): Result<String> = runCatching {
        val payload = ReportDataPayload.fromDomain(data)
        reportDataAdapter.toJson(payload)
    }

    private data class ReportDataPayload(
        val linkStatus: com.app.miklink.core.domain.model.report.LinkStatusData? = null,
        val tdr: List<com.app.miklink.core.domain.model.report.TdrEntry>? = null,
        val neighbors: List<com.app.miklink.core.domain.model.report.NeighborData>? = null,
        val pingSamples: List<com.app.miklink.core.domain.model.report.PingSample>? = null,
        val speedTest: com.app.miklink.core.domain.model.report.SpeedTestData? = null,
        val extra: Map<String, String>? = null
    ) {
        fun toDomain(): ReportData {
            return ReportData(
                linkStatus = linkStatus,
                tdr = tdr.orEmpty(),
                neighbors = neighbors.orEmpty(),
                pingSamples = pingSamples.orEmpty(),
                speedTest = speedTest,
                extra = extra.orEmpty()
            )
        }

        companion object {
            fun fromDomain(data: ReportData): ReportDataPayload {
                return ReportDataPayload(
                    linkStatus = data.linkStatus,
                    tdr = data.tdr,
                    neighbors = data.neighbors,
                    pingSamples = data.pingSamples,
                    speedTest = data.speedTest,
                    extra = data.extra
                )
            }
        }
    }
}

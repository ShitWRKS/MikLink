package com.app.miklink.data.pdf

import com.app.miklink.data.network.CableTestResult
import com.app.miklink.data.network.PingResult
import com.app.miklink.ui.history.model.ParsedResults
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small, focused parser to handle conversion/legacy normalization of test result JSON
 * extracted from PdfGeneratorIText. Keeping this logic isolated makes it easier to
 * unit test and reuse across the codebase.
 */
@Singleton
class ParsedResultsParser @Inject constructor(private val moshi: Moshi) {

    fun parse(json: String): ParsedResults? {
        if (json.isBlank()) return null

        var parsed: ParsedResults? = null
        try {
            parsed = moshi.adapter(ParsedResults::class.java).fromJson(json)
            if (parsed != null && parsed.ping != null && parsed.tdr != null) return parsed
        } catch (_: Exception) {
            // ignore - we'll try legacy normalisation below
        }

        return try {
            val mapType = com.squareup.moshi.Types.newParameterizedType(
                Map::class.java, String::class.java, Any::class.java
            )
            val mapAdapter: com.squareup.moshi.JsonAdapter<Map<String, Any?>> = moshi.adapter(mapType)
            val root = mapAdapter.fromJson(json)

            if (root == null) return parsed

            var pingList: MutableList<PingResult>? = null

            if (parsed?.ping.isNullOrEmpty()) {
                val listType = com.squareup.moshi.Types.newParameterizedType(
                    List::class.java, PingResult::class.java
                )
                val pingListAdapter: com.squareup.moshi.JsonAdapter<List<PingResult>> = moshi.adapter(listType)
                root.forEach { (key, value) ->
                    if (key.startsWith("ping_")) {
                        var items = pingListAdapter.fromJsonValue(value) ?: emptyList()
                        if (items.isEmpty()) {
                            val rawList = (value as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
                            if (rawList.isNotEmpty()) {
                                items = rawList.map { m ->
                                    PingResult(
                                        avgRtt = m["avg-rtt"] as? String,
                                        host = m["host"] as? String,
                                        maxRtt = m["max-rtt"] as? String,
                                        minRtt = m["min-rtt"] as? String,
                                        packetLoss = m["packet-loss"] as? String,
                                        received = m["received"] as? String,
                                        sent = m["sent"] as? String,
                                        seq = m["seq"] as? String,
                                        size = m["size"] as? String,
                                        time = m["time"] as? String,
                                        ttl = m["ttl"] as? String
                                    )
                                }
                            }
                        }
                        if (items.isNotEmpty()) {
                            if (pingList == null) pingList = mutableListOf()
                            pingList.addAll(items)
                        }
                    }
                }
            }

            var tdrList: List<CableTestResult>? = parsed?.tdr
            if (tdrList == null) {
                val tdrVal = root["tdr"]
                when (tdrVal) {
                    is Map<*, *> -> {
                        val tdrAdapter: com.squareup.moshi.JsonAdapter<CableTestResult> =
                            moshi.adapter(CableTestResult::class.java)
                        val single = tdrAdapter.fromJsonValue(tdrVal)
                        if (single != null) tdrList = listOf(single)
                    }
                    is List<*> -> {
                        val listType = com.squareup.moshi.Types.newParameterizedType(
                            List::class.java, CableTestResult::class.java
                        )
                        val listAdapter: com.squareup.moshi.JsonAdapter<List<CableTestResult>> = moshi.adapter(listType)
                        tdrList = listAdapter.fromJsonValue(tdrVal)
                    }
                }
            }

            if (pingList == null && tdrList == null) parsed else ParsedResults(
                tdr = tdrList ?: parsed?.tdr,
                link = parsed?.link,
                lldp = parsed?.lldp,
                ping = pingList ?: parsed?.ping,
                speedTest = parsed?.speedTest
            )
        } catch (e: Exception) {
            android.util.Log.e("ParsedResultsParser", "Error parsing results JSON (legacy)", e)
            parsed
        }
    }
}

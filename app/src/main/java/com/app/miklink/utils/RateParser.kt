package com.app.miklink.utils

import android.util.Log

object RateParser {
    private const val TAG = "RateParser"

    // Parse common rate representations to Mbps (integer)
    fun parseToMbps(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        val s = raw.trim().replace("\\s+".toRegex(), "").uppercase()
        return try {
            when {
                // Check generic suffixes FIRST (before specific ones)
                s.endsWith("GBPS") -> {
                    // Numeric like 1Gbps or 2.5Gbps or 0.001Gbps
                    val num = s.removeSuffix("GBPS").toDoubleOrNull()
                    if (num != null) (num * 1000).toInt() else 0
                }
                s.endsWith("MBPS") -> {
                    val num = s.removeSuffix("MBPS").toDoubleOrNull()
                    if (num != null) num.toInt() else 0
                }
                // Now check specific values
                s.endsWith("10G") || s.endsWith("10GB") || s == "10G" -> 10000
                s.endsWith("1G") || s.endsWith("1GB") || s == "1G" -> 1000
                s.endsWith("100M") || s.endsWith("100MB") || s == "100M" -> 100
                s.endsWith("10M") || s.endsWith("10MB") || s == "10M" -> 10
                s.endsWith("G") && s.length > 1 -> {
                    val num = s.removeSuffix("G").toDoubleOrNull()
                    if (num != null) (num * 1000).toInt() else 0
                }
                s.endsWith("M") && s.length > 1 -> {
                    val num = s.removeSuffix("M").toDoubleOrNull()
                    if (num != null) num.toInt() else 0
                }
                s.matches(Regex("\\d+")) -> s.toInt() // assume already Mbps
                s.matches(Regex("\\d+(\\.\\d+)?K")) -> { // kbps
                    val num = s.removeSuffix("K").toDoubleOrNull()
                    if (num != null) (num / 1000.0).toInt() else 0
                }
                else -> {
                    // Try to extract digits
                    val digits = s.filter { it.isDigit() }
                    digits.toIntOrNull() ?: run {
                        Log.w(TAG, "Unrecognized rate format: '$raw'")
                        0
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse rate '$raw': ${e.message}")
            0
        }
    }

    fun formatReadable(mbps: Int): String {
        return when {
            mbps >= 10000 -> "${mbps / 1000}Gbps"
            mbps >= 1000 -> "${mbps / 1000}Gbps"
            mbps >= 1 -> "${mbps}Mbps"
            else -> "-"
        }
    }
}


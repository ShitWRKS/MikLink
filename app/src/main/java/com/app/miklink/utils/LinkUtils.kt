package com.app.miklink.utils

/**
 * Normalizes link status strings for consistent display.
 * 
 * Logic:
 * - "link-ok", "up", "running", "ok" -> "Connesso"
 * - "no-link", "down" -> "Disconnesso"
 * - "unknown" -> "Sconosciuto"
 * - Everything else -> "N/A" (fallback)
 */
fun normalizeLinkStatus(status: String?): String {
    if (status.isNullOrBlank()) return "N/A"
    return when (status.lowercase().trim()) {
        "link-ok", "up", "running", "ok" -> "Connesso"
        "no-link", "down" -> "Disconnesso"
        "unknown" -> "Sconosciuto"
        else -> "N/A"
    }
}

/**
 * Normalizes link speed strings for consistent display.
 * 
 * Logic:
 * - Adds space between value and unit (e.g. "1Gbps" -> "1 Gbps")
 * - Supports Mbps, Gbps (up to 400Gbps)
 * - Retains original string if format is not recognized
 */
fun normalizeLinkSpeed(speed: String?): String {
    if (speed.isNullOrBlank()) return "N/A"
    // Remove extra spaces first
    val s = speed.trim().replace(" ", "")
    
    // Check if it ends with "bps" (case insensitive)
    return when {
        s.endsWith("bps", ignoreCase = true) -> {
            // Find where digits end and letters start
            val unitIndex = s.indexOfFirst { c -> c.isLetter() }
            if (unitIndex > 0) {
                "${s.substring(0, unitIndex)} ${s.substring(unitIndex)}"
            } else s
        }
        else -> s
    }
}

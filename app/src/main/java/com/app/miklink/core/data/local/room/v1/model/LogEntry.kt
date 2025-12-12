package com.app.miklink.core.data.local.room.v1.model

/**
 * Represents a single log entry from MikroTik RouterOS
 */
data class LogEntry(
    val time: String,      // Timestamp from router (e.g., "jan/02/2025 14:30:15")
    val topics: String,    // Log topics/categories (e.g., "system,info")
    val message: String,   // Log message content
    val timestamp: Long = System.currentTimeMillis()  // Local timestamp for ordering
)

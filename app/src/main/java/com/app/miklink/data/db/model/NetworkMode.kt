package com.app.miklink.data.db.model

enum class NetworkMode(val dbValue: String) {
    DHCP("DHCP"),
    STATIC("Static IP");

    companion object {
        fun fromDbValue(value: String?): NetworkMode {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: DHCP
        }
    }
}

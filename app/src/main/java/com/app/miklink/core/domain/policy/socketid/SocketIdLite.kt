/*
 * Purpose: Provide deterministic Socket-ID Lite formatting/parsing shared across UI and dashboard gap logic per ADR-0004.
 * Inputs: Prefix, separator, padding, suffix, numeric id to format, and existing socket names to parse.
 * Outputs: Stable socket-id strings, parsed numeric identifiers, and first-missing-positive calculation for auto-increment.
 * Notes: Separators render only when a part exists on each side (no trailing separator if suffix is blank, no leading if prefix is blank). Parsing tolerates legacy leading/trailing separators for backward compatibility.
 */
package com.app.miklink.core.domain.policy.socketid

import java.util.Locale

object SocketIdLite {

    fun format(
        prefix: String,
        separator: String,
        numberPadding: Int,
        suffix: String,
        idNumber: Int
    ): String {
        val padded = String.format(Locale.US, "%0${numberPadding}d", idNumber)
        val builder = StringBuilder()
        if (prefix.isNotEmpty()) {
            builder.append(prefix).append(separator)
        }
        builder.append(padded)
        if (suffix.isNotEmpty()) {
            builder.append(separator).append(suffix)
        }
        return builder.toString()
    }

    fun parseIdNumber(
        socketName: String,
        prefix: String,
        separator: String
    ): Int? {
        if (separator.isEmpty()) return null
        var current = socketName
        if (prefix.isNotEmpty()) {
            if (!current.startsWith(prefix)) return null
            current = current.removePrefix(prefix)
            if (!current.startsWith(separator)) return null
            current = current.removePrefix(separator)
        } else if (current.startsWith(separator)) {
            // legacy format tolerance: drop leading separator when no prefix
            current = current.removePrefix(separator)
        }

        val nextSeparatorIndex = current.indexOf(separator)
        val numberSegment = if (nextSeparatorIndex >= 0) {
            current.substring(0, nextSeparatorIndex)
        } else {
            current
        }
        if (numberSegment.isEmpty()) return null
        return numberSegment.toIntOrNull()
    }

    fun firstMissingPositive(existing: Collection<Int>): Int {
        if (existing.isEmpty()) return 1
        val ordered = existing.filter { it > 0 }.toSortedSet()
        var expected = 1
        for (value in ordered) {
            if (value != expected) return expected
            expected++
        }
        return expected
    }
}

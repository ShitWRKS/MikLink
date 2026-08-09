/*
 * Purpose: Sanitize execution log lines before they reach the UI by redacting credentials/tokens and truncating oversized content.
 * Inputs: Raw log strings that may contain sensitive tokens or long payloads.
 * Outputs: Safe, trimmed log strings with secrets replaced and overly long lines shortened.
 */
package com.app.miklink.core.domain.test.logging

class LogSanitizer(
    private val redactionToken: String = "<redacted>",
    private val maxLength: Int = MAX_LENGTH
) {
    fun sanitize(message: String): String {
        var safe = message.trim()
        if (safe.isEmpty()) return safe

        assignmentPatterns.forEach { pattern ->
            safe = pattern.replace(safe) { matchResult ->
                val prefix = matchResult.groups[1]?.value ?: ""
                "$prefix$redactionToken"
            }
        }

        safe = jsonSecretPattern.replace(safe) { matchResult ->
            val prefix = matchResult.groups[1]?.value.orEmpty()
            val suffix = matchResult.groups[3]?.value.orEmpty()
            "$prefix$redactionToken$suffix"
        }

        safe = urlUserInfoPattern.replace(safe) { matchResult ->
            val scheme = matchResult.groups[1]?.value.orEmpty()
            "$scheme$redactionToken@"
        }

        if (safe.length > maxLength) {
            safe = safe.take(maxLength) + " ...[truncated]"
        }

        return safe
    }

    /**
     * Recursively sanitizes trace payloads before they are serialized. Field-name
     * redaction and value inspection are both required because DTOs and error bodies
     * can contain serialized secrets under otherwise harmless keys such as `raw`.
     */
    fun sanitizeValue(value: Any?, key: String? = null, depth: Int = 0): Any? {
        if (key != null && isSensitiveKey(key)) return redactionToken
        if (value == null) return null
        if (depth >= MAX_DEPTH) return DEPTH_LIMIT_VALUE

        return when (value) {
            is Map<*, *> -> buildMap<String, Any?> {
                value.entries.take(MAX_COLLECTION_ITEMS).forEach { (childKey, childValue) ->
                    if (childKey != null) {
                        val childName = childKey.toString()
                        put(childName, sanitizeValue(childValue, childName, depth + 1))
                    }
                }
                if (value.size > MAX_COLLECTION_ITEMS) put(TRUNCATED_KEY, true)
            }
            is Iterable<*> -> value.take(MAX_COLLECTION_ITEMS)
                .map { sanitizeValue(it, key, depth + 1) }
                .let { sanitized ->
                    if (value.countAtMost(MAX_COLLECTION_ITEMS + 1) > MAX_COLLECTION_ITEMS) {
                        sanitized + COLLECTION_LIMIT_VALUE
                    } else {
                        sanitized
                    }
                }
            is Array<*> -> value.take(MAX_COLLECTION_ITEMS)
                .map { sanitizeValue(it, key, depth + 1) }
                .let { sanitized ->
                    if (value.size > MAX_COLLECTION_ITEMS) sanitized + COLLECTION_LIMIT_VALUE else sanitized
                }
            is String -> sanitize(value)
            is Number, is Boolean -> value
            else -> sanitize(value.toString())
        }
    }

    fun isSensitiveKey(key: String): Boolean {
        val normalized = key.lowercase().replace("-", "").replace("_", "")
        return SENSITIVE_KEY_PARTS.any(normalized::contains)
    }

    private fun Iterable<*>.countAtMost(limit: Int): Int {
        var count = 0
        val iterator = iterator()
        while (iterator.hasNext() && count < limit) {
            iterator.next()
            count++
        }
        return count
    }

    companion object {
        private const val MAX_LENGTH = 500
        private const val MAX_DEPTH = 8
        private const val MAX_COLLECTION_ITEMS = 100
        private const val DEPTH_LIMIT_VALUE = "<truncated-depth>"
        private const val COLLECTION_LIMIT_VALUE = "<truncated-items>"
        private const val TRUNCATED_KEY = "_truncated"
        private val SENSITIVE_KEY_PARTS = listOf(
            "password", "token", "secret", "authorization", "cookie", "privatekey"
        )
        private val assignmentPatterns = listOf(
            // password=secret
            Regex("(?i)(password\\s*=\\s*)([^;\\s,]+)"),
            // token=abcd
            Regex("(?i)(token\\s*=\\s*)([^;\\s,]+)"),
            // probePassword=xxx / probeToken=xxx
            Regex("(?i)(probe(password|token)\\s*=\\s*)([^;\\s,]+)"),
            // Authorization: Bearer xyz
            Regex("(?i)(authorization:\\s*)([^\\n\\r]+)"),
            // Generic secret=xxx
            Regex("(?i)(secret\\s*=\\s*)([^;\\s,]+)")
        )
        private val jsonSecretPattern = Regex(
            "(?i)([\\\"'](?:password|token|secret|authorization|cookie|private[_-]?key)[\\\"']\\s*:\\s*[\\\"'])(.*?)([\\\"'])"
        )
        private val urlUserInfoPattern = Regex("(?i)(https?://)[^/@\\s]+:[^/@\\s]+@")
    }
}

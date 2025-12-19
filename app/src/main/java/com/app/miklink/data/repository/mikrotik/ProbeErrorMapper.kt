/*
 * Purpose: Map low-level probe connectivity exceptions to user-facing messages without leaking sensitive details.
 * Inputs: Throwable from connectivity attempts and localized fallback messages.
 * Outputs: Human-readable error description, differentiating TLS handshake failures from generic errors.
 * Notes: Keeps HTTPS diagnostics centralized; does not alter security posture (no cipher weakening).
 */
package com.app.miklink.data.repository.mikrotik

import javax.net.ssl.SSLHandshakeException

internal object ProbeErrorMapper {
    fun toMessage(
        error: Throwable,
        defaultMessage: String,
        handshakeMessage: String
    ): String {
        return when (error) {
            is SSLHandshakeException -> handshakeMessage
            else -> error.message ?: defaultMessage
        }
    }
}

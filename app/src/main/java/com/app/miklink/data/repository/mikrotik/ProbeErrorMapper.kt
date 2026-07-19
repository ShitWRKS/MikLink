/*
 * Purpose: Map typed TestError from probe connectivity to user-facing messages without leaking sensitive details.
 * Inputs: TestError from MikroTikCallExecutor classification.
 * Outputs: Human-readable error description, differentiating TLS failures from generic errors.
 * Notes: Keeps HTTPS diagnostics centralized; does not alter security posture.
 */
package com.app.miklink.data.repository.mikrotik

import com.app.miklink.core.domain.test.model.TestError

internal object ProbeErrorMapper {
    fun toMessage(
        error: TestError,
        defaultMessage: String,
        handshakeMessage: String
    ): String {
        return when (error) {
            is TestError.Tls -> handshakeMessage
            else -> error.message.ifBlank { defaultMessage }
        }
    }
}

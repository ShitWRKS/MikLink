package com.app.miklink.core.domain.test.model

/**
 * Reason codes per StepResult.Skipped.
 * Usati per identificare in modo stabile il motivo dello skip, indipendentemente dalla lingua.
 *
 * Mapping UI: questi codici devono essere convertiti in stringhe localizzate nella UI.
 */
object TestSkipReason {
    // Ping
    const val PING_NO_TARGETS = "PING_NO_TARGETS"
    const val PING_NO_VALID_TARGETS = "PING_NO_VALID_TARGETS"
    
    // Speed Test
    const val SPEED_NO_SERVER = "SPEED_NO_SERVER"
    
    // Profile disabled
    const val PROFILE_DISABLED = "PROFILE_DISABLED"
    
    // Hardware unsupported
    const val HARDWARE_UNSUPPORTED = "HARDWARE_UNSUPPORTED"
}


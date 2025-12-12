package com.app.miklink.ui.test

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.app.miklink.R
import com.app.miklink.core.domain.test.model.TestSkipReason

/**
 * Mappa i reason codes di TestSkipReason alle stringhe localizzate.
 * Usato nella UI per mostrare i motivi degli skip in modo localizzato.
 */
object TestSkipReasonMapper {
    @StringRes
    private fun getStringResForReason(reason: String): Int {
        return when (reason) {
            TestSkipReason.PING_NO_TARGETS -> R.string.skip_reason_ping_no_targets
            TestSkipReason.PING_NO_VALID_TARGETS -> R.string.skip_reason_ping_no_valid_targets
            TestSkipReason.SPEED_NO_SERVER -> R.string.skip_reason_speed_no_server
            TestSkipReason.PROFILE_DISABLED -> R.string.skip_reason_profile_disabled
            TestSkipReason.HARDWARE_UNSUPPORTED -> R.string.skip_reason_hardware_unsupported
            else -> R.string.skip_reason_unknown
        }
    }
    
    /**
     * Converte un reason code in una stringa localizzata.
     * Se il reason non è un codice riconosciuto, ritorna il reason originale.
     */
    @Composable
    fun getLocalizedReason(reason: String): String {
        // Verifica se è un reason code standard
        val isStandardCode = reason.startsWith("PING_") || reason.startsWith("SPEED_") || 
                            reason == TestSkipReason.PROFILE_DISABLED || 
                            reason == TestSkipReason.HARDWARE_UNSUPPORTED
        
        return if (isStandardCode) {
            stringResource(getStringResForReason(reason))
        } else {
            // Fallback: se non è un reason code standard, ritorna il reason originale
            // (per compatibilità con vecchi report che potrebbero avere stringhe hardcoded)
            reason
        }
    }
}


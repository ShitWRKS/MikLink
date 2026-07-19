package com.app.miklink.utils

import com.app.miklink.core.domain.model.TdrCapability
import com.app.miklink.core.domain.model.TdrCapabilityClassifier

object Compatibility {

    /**
     * Checks if a given board name likely supports Cable Test (TDR).
     * Delegates to the documented TdrCapabilityClassifier (ADR-0013 Fase 3).
     *
     * @param boardName The name of the router board (e.g., "RB4011iGS+RM").
     * @return True if the model is SUPPORTED, false otherwise (UNSUPPORTED or UNKNOWN).
     */
    @Deprecated("Use TdrCapabilityClassifier.classify for typed capability")
    fun isTdrSupported(boardName: String?): Boolean {
        return TdrCapabilityClassifier.classify(boardName) == TdrCapability.SUPPORTED
    }
}

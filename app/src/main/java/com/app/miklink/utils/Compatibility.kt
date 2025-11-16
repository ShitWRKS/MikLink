package com.app.miklink.utils

object Compatibility {

    // Based on MikroTik official documentation and community feedback for TDR support
    private val tdrSupportedModels = setOf(
        // CCR Series (Cloud Core Routers)
        "CCR1009", "CCR1016", "CCR1036", "CCR1072",
        "CCR2004", "CCR2116", "CCR2216",

        // RB Series (RouterBOARDs)
        "RB4011", "RB1100", "RB2011", "RB3011", "RB951", "RB962", "RB750", "RB760",
        "RBmAP", "RBcAP", "RBwAP",

        // Other common models known to have TDR
        "hEX", "hAP"
    )

    // Keywords that indicate a model is likely NOT supported, even if it contains a supported prefix.
    private val tdrUnsupportedKeywords = setOf(
        "lite"
    )

    /**
     * Checks if a given board name likely supports Cable Test (TDR).
     * This check is based on a predefined set of models and partial matches.
     *
     * @param boardName The name of the router board (e.g., "RB4011iGS+RM").
     * @return True if the model is likely to support TDR, false otherwise.
     */
    fun isTdrSupported(boardName: String?): Boolean {
        if (boardName.isNullOrBlank()) return false

        // Check for unsupported keywords first.
        if (tdrUnsupportedKeywords.any { keyword -> boardName.contains(keyword, ignoreCase = true) }) {
            return false
        }

        // Check if the board name contains any of the known TDR-supported model identifiers.
        return tdrSupportedModels.any { model ->
            boardName.contains(model, ignoreCase = true)
        }
    }
}

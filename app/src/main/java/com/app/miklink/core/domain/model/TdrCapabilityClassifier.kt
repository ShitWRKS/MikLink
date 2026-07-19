/*
 * Purpose: Classify a MikroTik board-name into TdrCapability using ONLY the documented lists (ADR-0013 Fase 3).
 * Inputs: board-name string from RouterOS system resource.
 * Outputs: TdrCapability.SUPPORTED / UNSUPPORTED / UNKNOWN.
 * Notes:
 *  - Series matching only for the documented series.
 *  - No generic matching (hAP, hEX, CCR2004, ...).
 *  - Combo ports are UNSUPPORTED.
 *  - Unknown model => UNKNOWN (the runner attempts the real test).
 *  - UNSUPPORTED => SKIP; an unknown call error is NOT auto-classified as Unsupported.
 *  - "no-link" is a valid Layer 1 result, not a capability signal.
 */
package com.app.miklink.core.domain.model

object TdrCapabilityClassifier {

    // Series supported (prefix match on series token).
    // Tokens are matched against the board-name with separators removed so that
    // documented series like "OmniTIK 5 ac" or "RB450G x3" still match the series prefix.
    private val supportedSeries = setOf(
        "CCR1",      // CCR1xxx series
        "CRS1",      // CRS1xx series
        "CRS2",      // CRS2xx series
        "OMNITIK",   // OmniTIK series
        "RB450G",    // RB450G series
        "RB951",     // RB951 series
        "RB2011",    // RB2011 series
        "RB4011",    // RB4011 series
        "RB5009"     // RB5009 series (eth1 only; handled via model list below)
    )

    // Models or series supported (exact or documented prefix)
    private val supportedModels = setOf(
        "RB952Ui-5ac2nD",
        "RB962UiGS-5HacT2HnT",
        "RB1100AHx2",
        "RB1100x4",
        "RBD52G-5HacD2HnD",
        "RBD53G-5HacD2HnD",
        "RBcAPGi-5acD2nD",
        "RBmAPL-2nD",
        "RBmAP2nD",
        "RBwsAP-5Hac2nD",
        "RB3011UiAS-RM",
        "RB750Gr2",
        "RB750UPr2",
        "RB751U-2HnD",
        "RB850Gx2",
        "RBMetal 2SHPn",
        "RB931-2nD",
        "RB941-2nD",
        "RBDynaDishG-5HacD",
        "RBLDFG-5acD",
        "RBLHGG-5acD",
        "C52iG-5HaxD2HaxD",
        "C53UiG+5HPaxD2HPaxD",
        "S53UG+5HaxD2HPaxD",
        "H53UiG-5HaxQ2HaxQ"
    )

    // Combo ports are unsupported (e.g. models whose port is a combo SFP/RJ45)
    private val unsupportedComboPortModels: Set<String> = setOf(
        // Documented combo-port exclusions; matched as exact model tokens.
    )

    fun classify(boardName: String?): TdrCapability {
        if (boardName.isNullOrBlank()) return TdrCapability.UNKNOWN

        val name = boardName.trim()

        // Combo port exclusion
        if (unsupportedComboPortModels.any { name.equals(it, ignoreCase = true) }) {
            return TdrCapability.UNSUPPORTED
        }

        // Exact / documented model match
        if (supportedModels.any { name.equals(it, ignoreCase = true) || name.startsWith(it, ignoreCase = true) }) {
            return TdrCapability.SUPPORTED
        }

        // Series match only for documented series. Normalize by removing non-alphanumeric
        // separators so that "OmniTIK 5 ac" -> "OMNITIK5AC" matches the "OMNITIK" prefix.
        val normalized = name.uppercase().filter { it.isLetterOrDigit() }
        if (supportedSeries.any { normalized.startsWith(it.uppercase()) }) {
            return TdrCapability.SUPPORTED
        }

        return TdrCapability.UNKNOWN
    }
}

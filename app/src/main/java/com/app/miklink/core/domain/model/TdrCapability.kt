/*
 * Purpose: Typed TDR (cable-test) capability classification for a probe model.
 * Inputs: board-name / model string from RouterOS.
 * Outputs: SUPPORTED / UNSUPPORTED / UNKNOWN.
 * Notes: Classification uses ONLY the documented series/model lists from ADR-0013 Fase 3.
 *        No generic matching (hAP, hEX, CCR2004, ...). Unknown model => UNKNOWN (try real test).
 */
package com.app.miklink.core.domain.model

enum class TdrCapability {
    SUPPORTED,
    UNSUPPORTED,
    UNKNOWN
}

/*
 * Purpose: Pure-Kotlin port for the human-readable text emitted during a test run.
 * Inputs: Typed values (status, counts, names) needed to compose each message.
 * Outputs: Localized message strings; the domain never touches Android resources directly.
 * Notes: ADR-0013 keeps RunTestUseCase pure; the Android implementation lives outside the domain.
 *   Message semantics are preserved 1:1 with the previous R.string templates.
 */
package com.app.miklink.core.domain.test

interface TestRunTextProvider {
    fun resultCompleted(overallStatus: String): String
    fun initStarting(clientName: String, profileName: String, socketId: String): String
    fun labelInit(): String
    fun initLoading(): String
    fun linkChecking(): String
    fun linkStatus(status: String, linkState: String, rate: String): String
    fun linkFail(error: String): String
    fun linkSkip(reason: String): String
    fun tdrStarting(testInterface: String): String
    fun tdrStatus(status: String, entries: Int): String
    fun tdrFail(statusLabel: String, error: String): String
    fun tdrSkip(reason: String): String
    fun linkCableDisconnected(): String
    fun networkStarting(testInterface: String): String
    fun networkPass(mode: String, interfaceName: String): String
    fun networkFail(error: String): String
    fun networkSkip(reason: String): String
    fun lldpStarting(): String
    fun lldpPass(neighbors: Int): String
    fun lldpInfo(message: String): String
    fun lldpSkip(reason: String): String
    fun pingStarting(): String
    fun pingStatus(status: String, targets: Int, warnSuffix: String): String
    fun pingFail(error: String): String
    fun pingSkip(reason: String): String
    fun speedStarting(): String
    fun speedStatus(status: String, download: String, upload: String, warnSuffix: String): String
    fun speedFail(error: String): String
    fun speedSkip(reason: String): String
    fun resultError(error: String): String
}

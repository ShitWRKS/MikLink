/*
 * Purpose: Android implementation of TestRunTextProvider backed by R.string resources.
 * Inputs: Application context for resource lookup.
 * Outputs: Localized strings matching the legacy test-run messages.
 * Notes: Lives in the UI layer so the domain stays pure Kotlin (ADR-0013).
 */
package com.app.miklink.ui.test

import android.content.Context
import com.app.miklink.R
import com.app.miklink.core.domain.test.TestRunTextProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidTestRunTextProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TestRunTextProvider {

    override fun resultCompleted(overallStatus: String): String =
        context.getString(R.string.log_result_completed, overallStatus)

    override fun initStarting(clientName: String, profileName: String, socketId: String): String =
        context.getString(R.string.log_init_starting, clientName, profileName, socketId)

    override fun labelInit(): String =
        context.getString(R.string.log_label_init)

    override fun initLoading(): String =
        context.getString(R.string.log_init_loading)

    override fun linkChecking(): String =
        context.getString(R.string.log_link_checking)

    override fun linkStatus(status: String, linkState: String, rate: String): String =
        context.getString(R.string.log_link_status, status, linkState, rate)

    override fun linkFail(error: String): String =
        context.getString(R.string.log_link_fail, error)

    override fun linkSkip(reason: String): String =
        context.getString(R.string.log_link_skip, reason)

    override fun tdrStarting(testInterface: String): String =
        context.getString(R.string.log_tdr_starting, testInterface)

    override fun tdrStatus(status: String, entries: Int): String =
        context.getString(R.string.log_tdr_status, status, entries)

    override fun tdrFail(statusLabel: String, error: String): String =
        context.getString(R.string.log_tdr_fail, statusLabel, error)

    override fun tdrSkip(reason: String): String =
        context.getString(R.string.log_tdr_skip, reason)

    override fun linkCableDisconnected(): String =
        context.getString(R.string.log_link_cable_disconnected)

    override fun layer1FailedSkipping(): String =
        context.getString(R.string.log_layer1_failed_skipping)

    override fun networkStarting(testInterface: String): String =
        context.getString(R.string.log_network_starting, testInterface)

    override fun networkPass(mode: String, interfaceName: String): String =
        context.getString(R.string.log_network_pass, mode, interfaceName)

    override fun networkFail(error: String): String =
        context.getString(R.string.log_network_fail, error)

    override fun networkSkip(reason: String): String =
        context.getString(R.string.log_network_skip, reason)

    override fun lldpStarting(): String =
        context.getString(R.string.log_lldp_starting)

    override fun lldpPass(neighbors: Int): String =
        context.getString(R.string.log_lldp_pass, neighbors)

    override fun lldpInfo(message: String): String =
        context.getString(R.string.log_lldp_info, message)

    override fun lldpSkip(reason: String): String =
        context.getString(R.string.log_lldp_skip, reason)

    override fun pingStarting(): String =
        context.getString(R.string.log_ping_starting)

    override fun pingStatus(status: String, targets: Int, warnSuffix: String): String =
        context.getString(R.string.log_ping_status, status, targets, warnSuffix)

    override fun pingFail(error: String): String =
        context.getString(R.string.log_ping_fail, error)

    override fun pingSkip(reason: String): String =
        context.getString(R.string.log_ping_skip, reason)

    override fun speedStarting(): String =
        context.getString(R.string.log_speed_starting)

    override fun speedStatus(status: String, download: String, upload: String, warnSuffix: String): String =
        context.getString(R.string.log_speed_status, status, download, upload, warnSuffix)

    override fun speedFail(error: String): String =
        context.getString(R.string.log_speed_fail, error)

    override fun speedSkip(reason: String): String =
        context.getString(R.string.log_speed_skip, reason)

    override fun resultError(error: String): String =
        context.getString(R.string.log_result_error, error)
}

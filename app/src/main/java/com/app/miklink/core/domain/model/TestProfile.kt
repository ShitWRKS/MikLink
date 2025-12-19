/*
 * Purpose: Domain model for test profiles including enabled steps, ping targets, and user-configurable thresholds.
 * Inputs: Persisted profile fields plus optional description and ping target configuration.
 * Outputs: Immutable profile configuration consumed by test execution and threshold evaluation policies.
 * Notes: Thresholds default to sensible values; UI can override per profile.
 */
package com.app.miklink.core.domain.model

data class TestProfile(
    val profileId: Long,
    val profileName: String,
    val profileDescription: String?,
    val runTdr: Boolean,
    val runLinkStatus: Boolean,
    val runLldp: Boolean,
    val runPing: Boolean,
    val pingTarget1: String?,
    val pingTarget2: String?,
    val pingTarget3: String?,
    val pingCount: Int,
    val runSpeedTest: Boolean,
    val thresholds: TestThresholds = TestThresholds.defaults()
)

/*
 * Purpose: Persist test profiles without triggering UNIQUE constraint errors by routing edits to update.
 * Inputs: TestProfile instances with profileId set (0 for new profiles, existing id for edits).
 * Outputs: Identifier of the saved profile, delegating insert/update to the repository as appropriate.
 * Notes: Centralizes the upsert policy so UI layers do not call insert on existing primary keys.
 */
package com.app.miklink.core.domain.usecase.testprofile

import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.utils.NetworkValidator
import com.app.miklink.core.domain.validation.TestThresholdsValidator
import javax.inject.Inject

interface SaveTestProfileUseCase {
    suspend operator fun invoke(profile: TestProfile): Long
}

class SaveTestProfileUseCaseImpl @Inject constructor(
    private val testProfileRepository: TestProfileRepository
) : SaveTestProfileUseCase {
    override suspend fun invoke(profile: TestProfile): Long {
        validate(profile)
        return if (profile.profileId == 0L) {
            testProfileRepository.insertProfile(profile)
        } else {
            testProfileRepository.updateProfile(profile)
            profile.profileId
        }
    }

    private fun validate(profile: TestProfile) {
        if (profile.profileName.isBlank()) {
            throw IllegalArgumentException("Profile name is required")
        }

        val hasAtLeastOneTestEnabled = profile.runLinkStatus ||
            profile.runTdr ||
            profile.runLldp ||
            profile.runPing ||
            profile.runSpeedTest
        if (!hasAtLeastOneTestEnabled) {
            throw IllegalArgumentException("At least one test must be enabled")
        }

        TestThresholdsValidator.validate(profile.thresholds)

        if (!profile.runPing) return

        val targets = listOf(profile.pingTarget1, profile.pingTarget2, profile.pingTarget3)
        val hasInvalidTarget = targets.any { target ->
            !target.isNullOrBlank() && !NetworkValidator.isValidTarget(target)
        }
        if (hasInvalidTarget) {
            throw IllegalArgumentException("Invalid ping target")
        }

        if (profile.pingCount !in 1..20) {
            throw IllegalArgumentException("Ping count must be between 1 and 20")
        }
    }
}

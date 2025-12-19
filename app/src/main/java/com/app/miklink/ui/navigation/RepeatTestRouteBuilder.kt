/*
 * Purpose: Build the repeat-test navigation route using existing report data and profile lookup.
 * Inputs: TestProfileRepository to resolve profileId by profileName, target TestReport to repeat.
 * Outputs: Route string for NavGraph (test_execution/{clientId}/{profileId}/{socketName}) or null if data missing.
 */
package com.app.miklink.ui.navigation

import android.net.Uri
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestReport
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RepeatTestRouteBuilder @Inject constructor(
    private val profileRepository: TestProfileRepository
) {
    suspend fun build(report: TestReport?): String? {
        report ?: return null
        val profiles = profileRepository.observeAllProfiles().first()
        val profile = profiles.firstOrNull { it.profileName == report.profileName } ?: return null
        val clientId = report.clientId ?: return null
        val encodedSocket = Uri.encode(report.socketName ?: "")
        return "test_execution/$clientId/${profile.profileId}/$encodedSocket"
    }
}

package com.app.miklink.e2e.support

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.model.TestThresholds

data class CoreFixtures(
    val client: Client,
    val profile: TestProfile,
    val report: TestReport
)

class TestFixtureManager(
    sessionId: String,
    private val clients: ClientRepository,
    private val profiles: TestProfileRepository,
    private val reports: ReportRepository
) {
    private val prefix = "e2e-${safe(sessionId)}"
    private val ownedClients = linkedMapOf<Long, Client>()
    private val ownedProfiles = linkedMapOf<Long, TestProfile>()
    private val ownedReports = linkedMapOf<Long, TestReport>()

    var cleanupResult: CleanupResult = CleanupResult(CleanupStatus.NOT_REQUIRED)
        private set

    suspend fun createCoreFixtures(): CoreFixtures {
        val clientTemplate = defaultClient(prefix)
        val clientId = clients.insertClient(clientTemplate)
        val client = clientTemplate.copy(clientId = clientId).also { ownedClients[clientId] = it }

        val profileTemplate = defaultProfile(prefix)
        val profileId = profiles.insertProfile(profileTemplate)
        val profile = profileTemplate.copy(profileId = profileId).also { ownedProfiles[profileId] = it }

        val reportTemplate = defaultReport(prefix, clientId, profile.profileName)
        val reportId = reports.saveReport(reportTemplate)
        val report = reportTemplate.copy(reportId = reportId).also { ownedReports[reportId] = it }

        return CoreFixtures(client, profile, report)
    }

    suspend fun cleanup(): CleanupResult {
        val failures = mutableListOf<String>()
        ownedReports.toMap().forEach { (id, report) ->
            runCatching {
                if (reports.getReport(id) != null) reports.deleteReport(report)
                ownedReports.remove(id)
            }.onFailure { failures += "report:$id" }
        }
        ownedProfiles.toMap().forEach { (id, profile) ->
            runCatching {
                if (profiles.getProfile(id) != null) profiles.deleteProfile(profile)
                ownedProfiles.remove(id)
            }.onFailure { failures += "profile:$id" }
        }
        ownedClients.toMap().forEach { (id, client) ->
            runCatching {
                if (clients.getClient(id) != null) clients.deleteClient(client)
                ownedClients.remove(id)
            }.onFailure { failures += "client:$id" }
        }
        cleanupResult = if (failures.isEmpty()) {
            CleanupResult(CleanupStatus.PASS)
        } else {
            CleanupResult(CleanupStatus.FAIL, "FIXTURE_CLEANUP_FAILED:${failures.joinToString()}")
        }
        return cleanupResult
    }

    companion object {
        fun defaultClient(prefix: String) = Client(
            clientId = 0,
            companyName = "$prefix-client",
            location = "instrumentation",
            notes = "session-owned",
            networkMode = NetworkMode.DHCP,
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "1G",
            socketPrefix = "E2E",
            socketSuffix = "",
            socketSeparator = "-",
            socketNumberPadding = 3,
            nextIdNumber = 1,
            speedTestServerAddress = null,
            speedTestServerUser = null,
            speedTestServerPassword = null
        )

        fun defaultProfile(prefix: String) = TestProfile(
            profileId = 0,
            profileName = "$prefix-profile",
            profileDescription = "session-owned",
            runTdr = false,
            runLinkStatus = true,
            runLldp = false,
            runPing = false,
            pingTarget1 = null,
            pingTarget2 = null,
            pingTarget3 = null,
            pingCount = 1,
            runSpeedTest = false,
            thresholds = TestThresholds.defaults()
        )

        fun defaultReport(prefix: String, clientId: Long, profileName: String) = TestReport(
            clientId = clientId,
            timestamp = System.currentTimeMillis(),
            socketName = "$prefix-001",
            notes = "session-owned",
            probeName = null,
            profileName = profileName,
            overallStatus = "PASS",
            resultsJson = "{}"
        )

        private fun safe(value: String): String =
            value.replace(Regex("[^A-Za-z0-9-]"), "-").take(40).ifBlank { "session" }
    }
}

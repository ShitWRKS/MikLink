package com.app.miklink.data.repository

import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.di.AppMoshi
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManagerImpl @Inject constructor(
    private val probeRepository: ProbeRepository,
    private val testProfileRepository: TestProfileRepository,
    private val clientRepository: ClientRepository,
    private val reportRepository: ReportRepository,
    @AppMoshi private val moshi: Moshi,
    private val txRunner: com.app.miklink.core.data.transaction.TransactionRunner
) : BackupManager {

    override suspend fun exportConfigToJson(): String {
        val probe = probeRepository.getProbeConfig()
        val profiles = testProfileRepository.observeAllProfiles().first()
        val clients = clientRepository.observeAllClients().first()
        val reports = reportRepository.observeAllReports().first()

        // v2: opaque per-file unique reference for each client (no DB id, no UUID in DB).
        // Two clients with the same name+location get distinct refs via an ordinal suffix.
        fun baseKeyFor(client: com.app.miklink.core.domain.model.Client): String {
            val name = client.companyName.trim().lowercase().replace("\\s+".toRegex(), "_")
            val loc = (client.location ?: "").trim().lowercase().replace("\\s+".toRegex(), "_")
            return "$name|$loc"
        }
        val refCounts = mutableMapOf<String, Int>()
        val clientIdToRef = LinkedHashMap<Long, String>()
        clients.forEach { client ->
            val base = baseKeyFor(client)
            val ordinal = (refCounts[base] ?: 0) + 1
            refCounts[base] = ordinal
            clientIdToRef[client.clientId] = if (ordinal == 1) base else "$base#$ordinal"
        }

        val backupClients = clients.map { client ->
            com.app.miklink.data.repository.BackupClient(
                companyName = client.companyName,
                location = client.location,
                notes = client.notes,
                networkMode = client.networkMode,
                staticIp = client.staticIp,
                staticSubnet = client.staticSubnet,
                staticGateway = client.staticGateway,
                staticCidr = client.staticCidr,
                minLinkRate = client.minLinkRate,
                socketPrefix = client.socketPrefix,
                socketSuffix = client.socketSuffix,
                socketSeparator = client.socketSeparator,
                socketNumberPadding = client.socketNumberPadding,
                nextIdNumber = client.nextIdNumber,
                speedTestServerAddress = client.speedTestServerAddress,
                speedTestServerUser = client.speedTestServerUser,
                speedTestServerPassword = client.speedTestServerPassword,
                clientRef = clientIdToRef.getValue(client.clientId)
            )
        }

        val backupReports = reports.map { report ->
            com.app.miklink.data.repository.BackupReport(
                timestamp = report.timestamp,
                socketName = report.socketName,
                notes = report.notes,
                probeName = report.probeName,
                profileName = report.profileName,
                overallStatus = report.overallStatus,
                resultFormatVersion = report.resultFormatVersion,
                resultsJson = report.resultsJson,
                clientRef = report.clientId?.let { clientIdToRef[it] }
            )
        }
        val backupData = com.app.miklink.data.repository.BackupData(
            version = 2,
            probe = probe,
            clients = backupClients,
            profiles = profiles,
            reports = backupReports
        )
        val adapter = moshi.adapter(BackupData::class.java)
        return adapter.toJson(backupData)
    }

    override suspend fun importConfigFromJson(json: String): Result<Unit> {
        val adapter = moshi.adapter(BackupData::class.java)
        val backupData = try { adapter.fromJson(json) } catch (e: Exception) { null }
            ?: return Result.failure(BackupImportException("JSON malformato"))
        return importBackupData(backupData)
    }

    override suspend fun importBackupData(backupData: BackupData): Result<Unit> {
        // Version gate: only v1 and v2 are importable; anything else is rejected (ADR-0013).
        if (backupData.version != 1 && backupData.version != 2) {
            return Result.failure(BackupImportException("Versione backup non supportata: ${backupData.version}"))
        }

        // Basic structural validation
        if (backupData.probe != null) {
            if (backupData.probe.ipAddress.isBlank() || backupData.probe.username.isBlank()) {
                return Result.failure(BackupImportException("Dati sonda incompleti"))
            }
        }
        if (backupData.profiles.any { it.profileName.isBlank() }) {
            return Result.failure(BackupImportException("Dati profilo incompleti"))
        }
        if (backupData.clients.any { it.companyName.isBlank() }) {
            return Result.failure(BackupImportException("Dati client incompleti"))
        }
        if (backupData.reports.any { it.resultsJson.isBlank() }) {
            return Result.failure(BackupImportException("Dati report incompleti"))
        }

        // Resolve the per-version reference for each client and validate referential integrity.
        // v2 uses clientRef; v1 uses legacy clientKey.
        val refFor: (BackupClient) -> String? = if (backupData.version == 2) {
            { it.clientRef.ifBlank { null } }
        } else {
            { it.clientKey?.ifBlank { null } }
        }
        val reportRefFor: (BackupReport) -> String? = if (backupData.version == 2) {
            { it.clientRef?.ifBlank { null } }
        } else {
            { it.clientKey?.ifBlank { null } }
        }

        val refs = backupData.clients.mapNotNull(refFor)
        // Duplicate client reference => collision => reject.
        if (refs.size != refs.toSet().size) {
            return Result.failure(
                BackupImportException(
                    if (backupData.version == 2) "clientRef duplicati nel backup" else "clientKey duplicati nel backup v1"
                )
            )
        }
        val knownRefs = refs.toSet()
        // Report referencing a non-existent client => reject (orphan reports must have null ref).
        backupData.reports.forEach { r ->
            val ref = reportRefFor(r)
            if (ref != null && ref !in knownRefs) {
                return Result.failure(BackupImportException("Report con riferimento client inesistente: $ref"))
            }
        }

        // Run import inside a single transaction. The rollback is delegated to Room;
        // no manual post-failure restore is performed and rollback exceptions are not swallowed.
        return try {
            txRunner.runInTransaction {
                // Delete existing data (children first, then parents).
                reportRepository.observeAllReports().first().forEach { report ->
                    reportRepository.deleteReport(report)
                }
                testProfileRepository.observeAllProfiles().first().forEach { profile ->
                    testProfileRepository.deleteProfile(profile)
                }
                clientRepository.observeAllClients().first().forEach { client ->
                    clientRepository.deleteClient(client)
                }

                // Save singleton probe if present (keep existing if null). Credentials preserved.
                backupData.probe?.let { probeRepository.saveProbeConfig(it) }

                // Insert all clients and build reference -> newId map.
                val refToNewId = mutableMapOf<String, Long>()
                backupData.clients.forEach { client ->
                    val newId = clientRepository.insertClient(
                        com.app.miklink.core.domain.model.Client(
                            clientId = 0L,
                            companyName = client.companyName,
                            location = client.location,
                            notes = client.notes,
                            networkMode = client.networkMode,
                            staticIp = client.staticIp,
                            staticSubnet = client.staticSubnet,
                            staticGateway = client.staticGateway,
                            staticCidr = client.staticCidr,
                            minLinkRate = client.minLinkRate,
                            socketPrefix = client.socketPrefix,
                            socketSuffix = client.socketSuffix,
                            socketSeparator = client.socketSeparator,
                            socketNumberPadding = client.socketNumberPadding,
                            nextIdNumber = client.nextIdNumber,
                            speedTestServerAddress = client.speedTestServerAddress,
                            speedTestServerUser = client.speedTestServerUser,
                            speedTestServerPassword = client.speedTestServerPassword
                        )
                    )
                    refFor(client)?.let { refToNewId[it] = newId }
                }

                // Insert all profiles.
                backupData.profiles.forEach { profile ->
                    testProfileRepository.insertProfile(profile)
                }

                // Insert all reports, mapping the client reference to the new clientId (null = orphan).
                backupData.reports.forEach { r ->
                    val clientId = reportRefFor(r)?.let { refToNewId[it] }
                    reportRepository.saveReport(
                        com.app.miklink.core.domain.model.TestReport(
                            reportId = 0L,
                            clientId = clientId,
                            timestamp = r.timestamp,
                            socketName = r.socketName,
                            notes = r.notes,
                            probeName = r.probeName,
                            profileName = r.profileName,
                            overallStatus = r.overallStatus,
                            resultFormatVersion = r.resultFormatVersion,
                            resultsJson = r.resultsJson
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** Failure of backup import validation or execution. */
class BackupImportException(message: String) : Exception(message)

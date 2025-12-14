package com.app.miklink.data.repository

import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface BackupManager {
    suspend fun exportConfigToJson(): String
    suspend fun importConfigFromJson(json: String): Result<Unit>
    suspend fun importBackupData(backupData: BackupData): Result<Unit>
}

@Singleton
class BackupManagerImpl @Inject constructor(
    private val probeRepository: ProbeRepository,
    private val testProfileRepository: TestProfileRepository,
    private val moshi: Moshi,
    private val txRunner: com.app.miklink.data.repository.TransactionRunner
) : BackupManager {

    override suspend fun exportConfigToJson(): String {
        val probes = listOfNotNull(probeRepository.getProbeConfig())
        val profiles = testProfileRepository.observeAllProfiles().first()
        val backupData = com.app.miklink.data.repository.BackupData(probes = probes, profiles = profiles)
        val adapter = moshi.adapter(BackupData::class.java)
        return adapter.toJson(backupData)
    }

    override suspend fun importConfigFromJson(json: String): Result<Unit> {
        val adapter = moshi.adapter(BackupData::class.java)
        val backupData = try { adapter.fromJson(json) } catch (e: Exception) { null }
        if (backupData == null) return Result.failure(Exception("JSON malformato"))
        // Delegate to a method that imports the domain backup data atomically
        return importBackupData(backupData)
    }

    override suspend fun importBackupData(backupData: BackupData): Result<Unit> {
        // Basic validation
        if (backupData.probes.any { it.ipAddress.isBlank() || it.username.isBlank() }) {
            return Result.failure(Exception("Dati sonda incompleti"))
        }
        if (backupData.profiles.any { it.profileName.isBlank() }) {
            return Result.failure(Exception("Dati profilo incompleti"))
        }

        // Pre-export backup (so we can restore if needed)
        val currentBackupJson = exportConfigToJson()

        // Run import inside transaction
        return try {
            txRunner.runInTransaction {
                // Delete all existing profiles (probes are singleton, so just save the first one)
                // Delete all existing profiles
                testProfileRepository.observeAllProfiles().first().forEach { profile ->
                    testProfileRepository.deleteProfile(profile)
                }

                // Save the first probe (singleton pattern)
                if (backupData.probes.isNotEmpty()) {
                    probeRepository.saveProbeConfig(backupData.probes.first())
                }
                
                // Insert all profiles
                backupData.profiles.forEach { profile ->
                    testProfileRepository.insertProfile(profile)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // Optional automatic rollback by trying to restore previous backup
            try {
                val adapter = moshi.adapter(BackupData::class.java)
                val originalBackup = adapter.fromJson(currentBackupJson)
                if (originalBackup != null) {
                    txRunner.runInTransaction {
                        testProfileRepository.observeAllProfiles().first().forEach { profile ->
                            testProfileRepository.deleteProfile(profile)
                        }
                        if (originalBackup.probes.isNotEmpty()) {
                            probeRepository.saveProbeConfig(originalBackup.probes.first())
                        }
                        originalBackup.profiles.forEach { profile ->
                            testProfileRepository.insertProfile(profile)
                        }
                    }
                }
            } catch (ex: Exception) {
                // swallow second-level rollback exception but log if needed
            }
            Result.failure(e)
        }
    }
}

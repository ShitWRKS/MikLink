package com.app.miklink.domain.usecase.backup

import com.app.miklink.core.data.repository.BackupRepository
import com.app.miklink.data.repository.BackupData
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case that encapsulates the import of a backup JSON into application storage.
 * This use case orchestrates the domain logic and delegates persistence to the repository.
 */
interface ImportBackupUseCase {
    suspend fun execute(json: String): Result<Unit>
}

@Singleton
class ImportBackupUseCaseImpl @Inject constructor(
    private val repository: BackupRepository,
    private val moshi: Moshi
) : ImportBackupUseCase {
    override suspend fun execute(json: String): Result<Unit> {
        // Parse JSON into BackupData
        val adapter = moshi.adapter(BackupData::class.java)
        val backupData = try { adapter.fromJson(json) } catch (e: Exception) { null }
        if (backupData == null) return Result.failure(Exception("JSON malformato"))

        // Basic validation moved to UseCase to keep repository lean
        if (backupData.probes.any { it.ipAddress.isBlank() || it.username.isBlank() }) {
            return Result.failure(Exception("Dati sonda incompleti"))
        }
        if (backupData.profiles.any { it.profileName.isBlank() }) {
            return Result.failure(Exception("Dati profilo incompleti"))
        }

        // Delegate persistence to repository (which runs transaction)
        return repository.importBackupData(backupData)
    }
}

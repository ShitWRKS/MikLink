package com.app.miklink.core.domain.usecase.backup

import com.app.miklink.core.data.repository.BackupRepository
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
    private val repository: BackupRepository
) : ImportBackupUseCase {
    override suspend fun execute(json: String): Result<Unit> = repository.importConfigFromJson(json)
}


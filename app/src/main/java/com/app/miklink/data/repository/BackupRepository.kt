package com.app.miklink.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import com.app.miklink.data.repository.BackupManager

@Singleton
class BackupRepository @Inject constructor(
    private val manager: BackupManager
) : com.app.miklink.core.data.repository.BackupRepository {
    // Implementations for the core bridge interface (delegate to manager)
    override suspend fun exportConfigToJson(): String = manager.exportConfigToJson()
    override suspend fun importConfigFromJson(json: String): Result<Unit> = manager.importConfigFromJson(json)
    override suspend fun importBackupData(backupData: com.app.miklink.data.repository.BackupData): Result<Unit> = manager.importBackupData(backupData)
}

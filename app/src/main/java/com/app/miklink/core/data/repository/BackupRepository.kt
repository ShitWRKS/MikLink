package com.app.miklink.core.data.repository

import com.app.miklink.data.repository.BackupData

/**
 * Bridge interface for backup repository used by domain use cases.
 */
interface BackupRepository {
    suspend fun exportConfigToJson(): String
    suspend fun importConfigFromJson(json: String): Result<Unit>
    suspend fun importBackupData(backupData: BackupData): Result<Unit>
}

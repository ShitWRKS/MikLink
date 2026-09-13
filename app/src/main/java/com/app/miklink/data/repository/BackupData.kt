package com.app.miklink.data.repository

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.model.NetworkMode

/**
 * Backup format for export/import. Includes a version number for future extension.
 *
 * NOTE: Probe is a singleton (no id concept). Clients and Reports are represented
 * with backup-specific DTOs to avoid exposing DB ids.
 */
data class BackupReport(
    val timestamp: Long,
    val socketName: String?,
    val notes: String?,
    val probeName: String?,
    val profileName: String?,
    val overallStatus: String,
    val resultFormatVersion: Int = 1,
    val resultsJson: String,
    // v2: opaque per-file reference to the owning client (null = orphan report).
    val clientRef: String? = null,
    // v1 legacy field, kept only for backward-compatible import of version 1 backups.
    val clientKey: String? = null
)

data class BackupClient(
    val companyName: String,
    val location: String?,
    val notes: String?,
    val networkMode: NetworkMode,
    val staticIp: String?,
    val staticSubnet: String?,
    val staticGateway: String?,
    val staticCidr: String?,
    val minLinkRate: String,
    val socketPrefix: String,
    val socketSuffix: String,
    val socketSeparator: String,
    val socketNumberPadding: Int,
    val nextIdNumber: Int,
    val speedTestServerAddress: String?,
    val speedTestServerUser: String?,
    val speedTestServerPassword: String?,
    // v2: opaque per-file unique reference (primary identifier inside the backup file).
    val clientRef: String = "",
    // v1 legacy field, kept only for backward-compatible import of version 1 backups.
    val clientKey: String? = null
)

data class BackupData(
    val version: Int = 2,
    val probe: ProbeConfig?, // nullable singleton
    val clients: List<BackupClient> = emptyList(),
    val profiles: List<TestProfile> = emptyList(),
    val reports: List<BackupReport> = emptyList()
)

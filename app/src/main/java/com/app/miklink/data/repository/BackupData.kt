package com.app.miklink.data.repository

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile

/**
 * Backup format for export/import. Includes a version number for future extension.
 */
data class BackupData(
    val version: Int = 1,
    val probes: List<ProbeConfig>,
    val profiles: List<TestProfile>
)

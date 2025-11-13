package com.app.miklink.data.repository

import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.TestProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(val probes: List<ProbeConfig>, val profiles: List<TestProfile>)

@Singleton
class BackupRepository @Inject constructor(
    private val probeConfigDao: ProbeConfigDao,
    private val testProfileDao: TestProfileDao,
    private val moshi: Moshi
) {

    suspend fun exportConfigToJson(): String {
        val probes = probeConfigDao.getAllProbes().first()
        val profiles = testProfileDao.getAllProfiles().first()
        val backupData = BackupData(probes, profiles)
        
        val type = Types.newParameterizedType(BackupData::class.java)
        val adapter = moshi.adapter<BackupData>(type)
        return adapter.toJson(backupData)
    }

    suspend fun importConfigFromJson(json: String) {
        val type = Types.newParameterizedType(BackupData::class.java)
        val adapter = moshi.adapter<BackupData>(type)
        val backupData = adapter.fromJson(json)

        if (backupData != null) {
            // Clear existing data
            probeConfigDao.deleteAll()
            testProfileDao.deleteAll()
            
            // Insert new data
            probeConfigDao.insertAll(backupData.probes)
            testProfileDao.insertAll(backupData.profiles)
        }
    }
}

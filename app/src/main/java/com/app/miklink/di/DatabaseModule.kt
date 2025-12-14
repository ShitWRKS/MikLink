package com.app.miklink.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.miklink.data.local.room.MikLinkDatabase
import com.app.miklink.data.local.room.dao.TestProfileDao
import com.app.miklink.data.local.room.entity.TestProfileEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMikLinkDatabase(
        @ApplicationContext context: Context,
        testProfileDaoProvider: Provider<TestProfileDao>
    ): MikLinkDatabase {
        return Room.databaseBuilder(
            context,
            MikLinkDatabase::class.java,
            "miklink"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    addDefaultProfiles(testProfileDaoProvider.get())
                }
            }
        })
        .build()
    }

    private suspend fun addDefaultProfiles(testProfileDao: TestProfileDao) {
        testProfileDao.insert(
            TestProfileEntity(
                profileName = "Full Test",
                profileDescription = "TDR, Link, LLDP, and Ping",
                runTdr = true,
                runLinkStatus = true,
                runLldp = true,
                runPing = true,
                pingTarget1 = "DHCP_GATEWAY",
                pingTarget2 = "8.8.8.8",
                pingTarget3 = null,
                pingCount = 4,
                runSpeedTest = false
            )
        )
        testProfileDao.insert(
            TestProfileEntity(
                profileName = "Quick Test",
                profileDescription = "Link status and Ping",
                runTdr = false,
                runLinkStatus = true,
                runLldp = false,
                runPing = true,
                pingTarget1 = "DHCP_GATEWAY",
                pingTarget2 = null,
                pingTarget3 = null,
                pingCount = 4,
                runSpeedTest = false
            )
        )
    }

    @Provides
    fun provideClientDao(db: MikLinkDatabase) = db.clientDao()

    @Provides
    fun provideProbeConfigDao(db: MikLinkDatabase) = db.probeConfigDao()

    @Provides
    fun provideTestReportDao(db: MikLinkDatabase) = db.testReportDao()

    @Provides
    fun provideTestProfileDao(db: MikLinkDatabase) = db.testProfileDao()
}
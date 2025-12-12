package com.app.miklink.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.miklink.core.data.local.room.v1.AppDatabase
import com.app.miklink.core.data.local.room.v1.dao.TestProfileDao
import com.app.miklink.core.data.local.room.v1.migration.Migrations
import com.app.miklink.core.data.local.room.v1.model.TestProfile
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
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        // Use a Provider to avoid circular dependency
        testProfileDaoProvider: Provider<TestProfileDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "miklink-db"
        )
        .addMigrations(*Migrations.ALL_MIGRATIONS)
        // Avoid full destructive fallback on all version mismatches to prevent data loss.
        // Keep fallback only for very old versions where we cannot reasonably support migration.
        .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Coroutine scope for the callback
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
            TestProfile(
                profileName = "Full Test",
                profileDescription = "TDR, Link, LLDP, and Ping",
                runTdr = true,
                runLinkStatus = true,
                runLldp = true,
                runPing = true,
                pingTarget1 = "DHCP_GATEWAY",
                pingTarget2 = "8.8.8.8"
            )
        )
        testProfileDao.insert(
            TestProfile(
                profileName = "Quick Test",
                profileDescription = "Link status and Ping",
                runTdr = false,
                runLinkStatus = true,
                runLldp = false,
                runPing = true,
                pingTarget1 = "DHCP_GATEWAY"
            )
        )
    }

    @Provides
    fun provideClientDao(db: AppDatabase) = db.clientDao()

    @Provides
    fun provideProbeConfigDao(db: AppDatabase) = db.probeConfigDao()

    @Provides
    fun provideReportDao(db: AppDatabase) = db.reportDao()

    @Provides
    fun provideTestProfileDao(db: AppDatabase) = db.testProfileDao()
}
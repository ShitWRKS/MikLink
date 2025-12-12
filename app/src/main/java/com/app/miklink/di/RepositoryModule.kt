package com.app.miklink.di

import com.app.miklink.data.repository.RouteManager
import com.app.miklink.data.repository.RouteManagerImpl
import com.app.miklink.data.repository.BackupManager
import com.app.miklink.data.repository.BackupManagerImpl
import com.app.miklink.data.repository.TransactionRunner
import com.app.miklink.data.repository.RoomTransactionRunner
import com.app.miklink.data.db.AppDatabase
import dagger.Provides
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
// removed duplicate import
import com.app.miklink.domain.usecase.backup.ImportBackupUseCase
import com.app.miklink.domain.usecase.backup.ImportBackupUseCaseImpl
import com.app.miklink.data.io.FileReader
import com.app.miklink.data.io.ContentResolverFileReader
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRouteManager(impl: RouteManagerImpl): RouteManager
    @Binds
    abstract fun bindBackupManager(impl: BackupManagerImpl): BackupManager

    @Binds
    abstract fun bindImportBackupUseCase(impl: ImportBackupUseCaseImpl): ImportBackupUseCase

    companion object {
        @Provides
        fun provideTransactionRunner(db: AppDatabase): TransactionRunner = RoomTransactionRunner(db)

        @Provides
        fun provideContentResolverFileReader(@ApplicationContext context: Context): FileReader = ContentResolverFileReader(context)

        @Provides
        @Singleton
        fun provideAppRepositoryLegacy(
            @ApplicationContext context: Context,
            clientDao: com.app.miklink.data.db.dao.ClientDao,
            probeConfigDao: com.app.miklink.data.db.dao.ProbeConfigDao,
            testProfileDao: com.app.miklink.data.db.dao.TestProfileDao,
            reportDao: com.app.miklink.data.db.dao.ReportDao,
            serviceFactory: com.app.miklink.data.network.MikroTikServiceFactory,
            routeManager: RouteManager,
            userPreferencesRepository: com.app.miklink.data.repository.UserPreferencesRepository
        ): com.app.miklink.data.repository.AppRepository_legacy {
            return com.app.miklink.data.repository.AppRepository_legacy(
                context,
                clientDao,
                probeConfigDao,
                testProfileDao,
                reportDao,
                serviceFactory,
                routeManager,
                userPreferencesRepository
            )
        }

        @Provides
        @Singleton
        fun provideAppRepositoryBridge(legacy: com.app.miklink.data.repository.AppRepository_legacy): com.app.miklink.core.data.repository.AppRepository = legacy
        
        @Provides
        @Singleton
        fun provideBackupRepositoryBridge(impl: com.app.miklink.data.repository.BackupRepository): com.app.miklink.core.data.repository.BackupRepository = impl
    }
}

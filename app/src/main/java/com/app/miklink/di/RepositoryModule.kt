package com.app.miklink.di

import com.app.miklink.data.repository.RouteManager
import com.app.miklink.data.repository.RouteManagerImpl
import com.app.miklink.data.repository.BackupManager
import com.app.miklink.data.repository.BackupManagerImpl
import com.app.miklink.data.repository.TransactionRunner
import com.app.miklink.data.repository.RoomTransactionRunner
import com.app.miklink.core.data.local.room.v1.AppDatabase
import com.app.miklink.core.data.local.room.v1.dao.ClientDao
import com.app.miklink.core.data.local.room.v1.dao.ProbeConfigDao
import com.app.miklink.core.data.local.room.v1.dao.ReportDao
import com.app.miklink.core.data.local.room.v1.dao.TestProfileDao
import dagger.Provides
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
// removed duplicate import
import com.app.miklink.domain.usecase.backup.ImportBackupUseCase
import com.app.miklink.domain.usecase.backup.ImportBackupUseCaseImpl
import com.app.miklink.core.data.io.FileReader
import com.app.miklink.core.data.io.impl.ContentResolverFileReader
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.data.repository.test.NetworkConfigRepository
import com.app.miklink.core.data.repository.test.PingTargetResolver
import com.app.miklink.data.repositoryimpl.roomv1.RoomV1ClientRepository
import com.app.miklink.data.repositoryimpl.roomv1.RoomV1ProbeRepository
import com.app.miklink.data.repositoryimpl.roomv1.RoomV1ReportRepository
import com.app.miklink.data.repositoryimpl.roomv1.RoomV1TestProfileRepository
import com.app.miklink.data.repositoryimpl.mikrotik.MikroTikTestRepositoryImpl
import com.app.miklink.data.repositoryimpl.NetworkConfigRepositoryImpl
import com.app.miklink.data.repositoryimpl.PingTargetResolverImpl

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

    // S5: Repository bindings
    @Binds
    @Singleton
    abstract fun bindClientRepository(impl: RoomV1ClientRepository): ClientRepository

    @Binds
    @Singleton
    abstract fun bindProbeRepository(impl: RoomV1ProbeRepository): ProbeRepository

    @Binds
    @Singleton
    abstract fun bindTestProfileRepository(impl: RoomV1TestProfileRepository): TestProfileRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: RoomV1ReportRepository): ReportRepository

    @Binds
    @Singleton
    abstract fun bindMikroTikTestRepository(impl: MikroTikTestRepositoryImpl): MikroTikTestRepository

    @Binds
    @Singleton
    abstract fun bindNetworkConfigRepository(impl: NetworkConfigRepositoryImpl): NetworkConfigRepository

    @Binds
    @Singleton
    abstract fun bindPingTargetResolver(impl: PingTargetResolverImpl): PingTargetResolver

    companion object {
        @Provides
        fun provideTransactionRunner(db: AppDatabase): TransactionRunner = RoomTransactionRunner(db)

        @Provides
        fun provideContentResolverFileReader(@ApplicationContext context: Context): FileReader = ContentResolverFileReader(context)

        @Provides
        @Singleton
        fun provideAppRepositoryLegacy(
            @ApplicationContext context: Context,
            clientDao: ClientDao,
            probeConfigDao: ProbeConfigDao,
            testProfileDao: TestProfileDao,
            reportDao: ReportDao,
            serviceFactory: com.app.miklink.core.data.remote.mikrotik.infra.MikroTikServiceFactory,
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

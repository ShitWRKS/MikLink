/*
 * Purpose: Central DI bindings for repositories, platform bridges, and selected use cases.
 * Inputs: Data-layer implementations and platform services.
 * Outputs: Bound interfaces for core/domain consumption across the app.
 * Notes: This module will be split further as the DI modularization progresses.
 */
package com.app.miklink.di

import android.content.Context
import com.app.miklink.core.data.io.DocumentReader
import com.app.miklink.core.data.io.DocumentWriter
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.data.repository.probe.ProbeConnectivityRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.probe.ProbeStatusRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.DhcpGatewayRepository
import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.data.repository.test.NetworkConfigRepository
import com.app.miklink.core.data.repository.test.PingTargetResolver
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.usecase.backup.ImportBackupUseCase
import com.app.miklink.core.domain.usecase.backup.ImportBackupUseCaseImpl
import com.app.miklink.core.domain.usecase.preferences.ObserveIdNumberingStrategyUseCase
import com.app.miklink.core.domain.usecase.preferences.ObserveIdNumberingStrategyUseCaseImpl
import com.app.miklink.core.domain.usecase.preferences.SetIdNumberingStrategyUseCase
import com.app.miklink.core.domain.usecase.preferences.SetIdNumberingStrategyUseCaseImpl
import com.app.miklink.data.io.AndroidDocumentReader
import com.app.miklink.data.io.AndroidDocumentWriter
import com.app.miklink.data.local.room.MikLinkDatabase
import com.app.miklink.data.preferences.UserPreferencesRepositoryImpl
import com.app.miklink.data.remote.mikrotik.MikroTikServiceProviderImpl
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.data.repository.BackupManager
import com.app.miklink.data.repository.BackupManagerImpl
import com.app.miklink.data.repository.DefaultBackupRepository
import com.app.miklink.data.repository.RouteManager
import com.app.miklink.data.repository.RouteManagerImpl
import com.app.miklink.data.repository.RoomTransactionRunner
import com.app.miklink.data.repository.TransactionRunner
import com.app.miklink.data.repositoryimpl.PingTargetResolverImpl
import com.app.miklink.data.repository.mikrotik.MikroTikDhcpGatewayRepository
import com.app.miklink.data.repository.mikrotik.MikroTikNetworkConfigRepository
import com.app.miklink.data.repository.mikrotik.MikroTikProbeConnectivityRepository
import com.app.miklink.data.repository.mikrotik.MikroTikProbeStatusRepository
import com.app.miklink.data.repository.mikrotik.MikroTikTestRepositoryRemote
import com.app.miklink.data.repositoryimpl.room.RoomClientRepository
import com.app.miklink.data.repositoryimpl.room.RoomProbeRepository
import com.app.miklink.data.repositoryimpl.room.RoomReportRepository
import com.app.miklink.data.repositoryimpl.room.RoomTestProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    @Binds
    abstract fun bindObserveIdNumberingStrategyUseCase(impl: ObserveIdNumberingStrategyUseCaseImpl): ObserveIdNumberingStrategyUseCase

    @Binds
    abstract fun bindSetIdNumberingStrategyUseCase(impl: SetIdNumberingStrategyUseCaseImpl): SetIdNumberingStrategyUseCase

    @Binds
    @Singleton
    abstract fun bindClientRepository(impl: RoomClientRepository): ClientRepository

    @Binds
    @Singleton
    abstract fun bindProbeRepository(impl: RoomProbeRepository): ProbeRepository

    @Binds
    @Singleton
    abstract fun bindTestProfileRepository(impl: RoomTestProfileRepository): TestProfileRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: RoomReportRepository): ReportRepository

    @Binds
    @Singleton
    abstract fun bindMikroTikTestRepository(impl: MikroTikTestRepositoryRemote): MikroTikTestRepository

    @Binds
    @Singleton
    abstract fun bindNetworkConfigRepository(impl: MikroTikNetworkConfigRepository): NetworkConfigRepository

    @Binds
    @Singleton
    abstract fun bindPingTargetResolver(impl: PingTargetResolverImpl): PingTargetResolver

    @Binds
    @Singleton
    abstract fun bindMikroTikServiceProvider(impl: MikroTikServiceProviderImpl): MikroTikServiceProvider

    @Binds
    @Singleton
    abstract fun bindDhcpGatewayRepository(impl: MikroTikDhcpGatewayRepository): DhcpGatewayRepository

    @Binds
    @Singleton
    abstract fun bindProbeStatusRepository(impl: MikroTikProbeStatusRepository): ProbeStatusRepository

    @Binds
    @Singleton
    abstract fun bindProbeConnectivityRepository(impl: MikroTikProbeConnectivityRepository): ProbeConnectivityRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    companion object {
        @Provides
        fun provideTransactionRunner(db: MikLinkDatabase): TransactionRunner = RoomTransactionRunner(db)

        @Provides
        @Singleton
        fun provideBackupRepositoryBridge(
            impl: DefaultBackupRepository
        ): com.app.miklink.core.data.repository.BackupRepository = impl

        @Provides
        @Singleton
        fun provideDocumentReader(@ApplicationContext context: Context): DocumentReader = AndroidDocumentReader(context)

        @Provides
        @Singleton
        fun provideDocumentWriter(@ApplicationContext context: Context): DocumentWriter = AndroidDocumentWriter(context)
    }
}

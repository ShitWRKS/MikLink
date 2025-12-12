package com.app.miklink.di

import com.app.miklink.core.domain.test.step.*
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.core.domain.usecase.test.RunTestUseCaseImpl
import com.app.miklink.data.teststeps.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI Module per Test Runner (UseCase + Step implementations).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TestRunnerModule {
    @Binds
    @Singleton
    abstract fun bindRunTestUseCase(impl: RunTestUseCaseImpl): RunTestUseCase

    @Binds
    abstract fun bindNetworkConfigStep(impl: NetworkConfigStepImpl): NetworkConfigStep

    @Binds
    abstract fun bindLinkStatusStep(impl: LinkStatusStepImpl): LinkStatusStep

    @Binds
    abstract fun bindCableTestStep(impl: CableTestStepImpl): CableTestStep

    @Binds
    abstract fun bindNeighborDiscoveryStep(impl: NeighborDiscoveryStepImpl): NeighborDiscoveryStep

    @Binds
    abstract fun bindPingStep(impl: PingStepImpl): PingStep

    @Binds
    abstract fun bindSpeedTestStep(impl: SpeedTestStepImpl): SpeedTestStep
}


package com.app.miklink.di

import com.app.miklink.core.domain.test.logging.DebugTraceSink
import com.app.miklink.core.domain.test.logging.DebugTraceSinkImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugTraceModule {
    @Binds
    @Singleton
    abstract fun bindDebugTraceSink(impl: DebugTraceSinkImpl): DebugTraceSink
}

package com.app.miklink.di

import com.app.miklink.core.data.report.ReportResultsCodec
import com.app.miklink.core.domain.usecase.report.ParseReportResultsUseCase
import com.app.miklink.core.domain.usecase.report.ParseReportResultsUseCaseImpl
import com.app.miklink.data.report.codec.MoshiReportResultsCodec
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportModule {

    @Binds
    @Singleton
    abstract fun bindReportResultsCodec(impl: MoshiReportResultsCodec): ReportResultsCodec

    @Binds
    abstract fun bindParseReportResultsUseCase(impl: ParseReportResultsUseCaseImpl): ParseReportResultsUseCase
}

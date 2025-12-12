package com.app.miklink.di

import com.app.miklink.core.data.pdf.PdfGenerator
import com.app.miklink.core.data.pdf.impl.PdfGeneratorIText
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfModule {

    @Binds
    @Singleton
    abstract fun bindPdfGenerator(impl: PdfGeneratorIText): PdfGenerator
}

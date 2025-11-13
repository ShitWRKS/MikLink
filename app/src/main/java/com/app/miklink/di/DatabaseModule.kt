package com.app.miklink.di

import android.content.Context
import androidx.room.Room
import com.app.miklink.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "miklink-db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideClientDao(db: AppDatabase) = db.clientDao()

    @Provides
    fun provideProbeConfigDao(db: AppDatabase) = db.probeConfigDao()

    @Provides
    fun provideReportDao(db: AppDatabase) = db.reportDao()

    @Provides
    fun provideTestProfileDao(db: AppDatabase) = db.testProfileDao()
}

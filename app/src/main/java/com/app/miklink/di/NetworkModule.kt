package com.app.miklink.di

import android.annotation.SuppressLint
import com.app.miklink.data.json.LinkedHashMapJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Strict Moshi for application codecs: backup, report, app-level serialization.
     * No global Boolean/Int adapters, no global coercion (ADR-0013).
     */
    @Provides
    @Singleton
    @AppMoshi
    fun provideAppMoshi(): Moshi {
        return Moshi.Builder()
            .add(LinkedHashMapJsonAdapterFactory)
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Moshi used ONLY by the RouterOS Retrofit converter. Permissive enough for RouterOS
     * quirks (e.g. NeighborDetail list adapter) but isolated from app codecs.
     */
    @Provides
    @Singleton
    @RouterOsMoshi
    fun provideRouterOsMoshi(): Moshi {
        return Moshi.Builder()
            .add(com.app.miklink.data.remote.mikrotik.infra.NeighborDetailListAdapter())
            .add(LinkedHashMapJsonAdapterFactory)
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // Timeout: massimo 60s per request come richiesto
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofitBuilder(@RouterOsMoshi moshi: Moshi): Retrofit.Builder {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
    }
}

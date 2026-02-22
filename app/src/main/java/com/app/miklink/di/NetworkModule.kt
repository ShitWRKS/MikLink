package com.app.miklink.di

import android.annotation.SuppressLint
import com.app.miklink.data.json.LinkedHashMapJsonAdapterFactory
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(com.app.miklink.data.remote.mikrotik.infra.NeighborDetailListAdapter())
            .add(object {
                @FromJson
                fun fromJson(reader: com.squareup.moshi.JsonReader): Boolean {
                    return if (reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                        reader.nextString().equals("true", ignoreCase = true)
                    } else {
                        reader.nextBoolean()
                    }
                }
                @ToJson
                fun toJson(writer: com.squareup.moshi.JsonWriter, value: Boolean) {
                    writer.value(value)
                }
            })
            .add(object {
                @FromJson
                fun fromJson(reader: com.squareup.moshi.JsonReader): Int {
                    return if (reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                        reader.nextString().toIntOrNull() ?: 0
                    } else {
                        reader.nextInt()
                    }
                }
                @ToJson
                fun toJson(writer: com.squareup.moshi.JsonWriter, value: Int) {
                    writer.value(value)
                }
            })
            .add(LinkedHashMapJsonAdapterFactory)
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // Unsafe trust manager helper removed from global module: trust-all is applied only per-probe

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
    fun provideRetrofitBuilder(moshi: Moshi): Retrofit.Builder {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
    }
}

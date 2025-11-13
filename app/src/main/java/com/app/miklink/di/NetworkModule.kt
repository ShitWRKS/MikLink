package com.app.miklink.di

import android.annotation.SuppressLint
import com.app.miklink.data.network.AuthInterceptor
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.cert.X509Certificate
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor = AuthInterceptor()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
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
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @SuppressLint("CustomX509TrustManager")
    private fun createUnsafeTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)

        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(createUnsafeTrustManager())
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        // Create an ssl socket factory with our all-trusting manager
        builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier { _, _ -> true }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    @Provides
    @Singleton
    fun provideRetrofitBuilder(moshi: Moshi): Retrofit.Builder {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
    }
}
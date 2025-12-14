package com.app.miklink.core.data.remote.mikrotik.infra

import com.app.miklink.core.domain.model.ProbeConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikApiService

@Singleton
class MikroTikServiceFactory @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder,
    private val defaultClient: OkHttpClient
) {

    /**
     * Create a configured MikroTikApiService for the given probe configuration.
     * The factory uses the probe ipAddress and isHttps flag to build the baseUrl,
     * and applies a basic-auth header if credentials are present.
     */
    fun createService(
        probe: ProbeConfig,
        socketFactory: javax.net.SocketFactory? = null
    ): MikroTikApiService {
        val scheme = if (probe.isHttps) "https" else "http"
        val baseUrl = "$scheme://${probe.ipAddress}/"

        val client = defaultClient.newBuilder().apply {
            if (probe.username.isNotBlank() || probe.password.isNotBlank()) {
                val authHeader = basicAuthHeader(probe.username, probe.password)
                addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .addHeader("Authorization", authHeader)
                        .build()
                    chain.proceed(req)
                }
            }
            if (socketFactory != null) {
                socketFactory(socketFactory)
            }
        }.build()

        val retrofit = retrofitBuilder
            .baseUrl(baseUrl)
            .client(client)
            .build()

        return retrofit.create(MikroTikApiService::class.java)
    }

    /**
     * Public helper used by tests to generate standard Basic auth header.
     */
    fun basicAuthHeader(user: String, pass: String): String {
        val token = java.util.Base64.getEncoder().encodeToString("$user:$pass".toByteArray(Charsets.UTF_8))
        return "Basic $token"
    }
}

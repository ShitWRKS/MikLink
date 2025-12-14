package com.app.miklink.data.remote.mikrotik.infra

import com.app.miklink.core.domain.model.ProbeConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService

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
            // If the probe is configured to use HTTPS, allow connecting to self-signed
            // MikroTik devices by using a permissive TrustManager on this client only.
            // Note: keep this logic local to avoid introducing new global abstractions.
            if (probe.isHttps) {
                val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                    object : javax.net.ssl.X509TrustManager {
                        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    }
                )
                val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                hostnameVerifier { _, _ -> true }
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

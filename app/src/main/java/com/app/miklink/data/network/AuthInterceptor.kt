package com.app.miklink.data.network

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    private var user: String = ""
    private var pass: String = ""

    fun setCredentials(user: String, pass: String) {
        this.user = user
        this.pass = pass
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (user.isBlank() && pass.isBlank()) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", Credentials.basic(user, pass))
            .build()

        return chain.proceed(newRequest)
    }
}

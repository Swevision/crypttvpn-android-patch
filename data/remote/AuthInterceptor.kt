package com.crypttvpn.data.remote

import com.crypttvpn.data.storage.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val storage: SecureStorage,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.contains("/internal/")) return chain.proceed(request)

        val token = storage.token
        val authed = if (token.isNullOrEmpty()) request
        else request.newBuilder().header("Authorization", "Bearer $token").build()

        val response = chain.proceed(authed)
        response.header("X-New-Token")?.let { storage.token = it }
        return response
    }
}

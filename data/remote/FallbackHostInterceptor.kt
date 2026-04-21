package com.crypttvpn.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

/**
 * Если основной api.crypttvpn.com недоступен (заблокирован) — повторяем запрос на api.crypttvpn.xyz.
 * Последний рабочий host запоминается на 5 минут.
 */
class FallbackHostInterceptor(
    private val hosts: List<String> = listOf("api.crypttvpn.com", "api.crypttvpn.xyz"),
) : Interceptor {

    private data class Cached(val host: String, val validUntil: Long)

    private val lastGood = AtomicReference<Cached?>(null)
    private val ttlMs = 5 * 60 * 1000L

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val orderedHosts = run {
            val good = lastGood.get()
            if (good != null && System.currentTimeMillis() < good.validUntil) {
                listOf(good.host) + hosts.filter { it != good.host }
            } else hosts
        }

        var lastError: IOException? = null
        for (host in orderedHosts) {
            val url = req.url.newBuilder().host(host).build()
            val rewritten = req.newBuilder().url(url).build()
            try {
                val resp = chain.proceed(rewritten)
                if (resp.isSuccessful || resp.code in 400..499) {
                    lastGood.set(Cached(host, System.currentTimeMillis() + ttlMs))
                    return resp
                }
                resp.close()
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError ?: IOException("All hosts unreachable")
    }

    companion object {
        fun baseUrl(): String = "https://api.crypttvpn.com/"
    }
}

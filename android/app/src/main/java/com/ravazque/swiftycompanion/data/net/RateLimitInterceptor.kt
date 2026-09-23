package com.ravazque.swiftycompanion.data.net

import okhttp3.Interceptor
import okhttp3.Response

// The API allows 2 requests per second per application: calls are spaced out and a 429
// is retried after the delay the server asks for.
class RateLimitInterceptor(
    private val minIntervalMs: Long = 550,
    private val maxRetries: Int = 2,
    private val sleep: (Long) -> Unit = Thread::sleep,
) : Interceptor {
    private var lastCallAt = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        while (true) {
            awaitSlot()
            val response = chain.proceed(chain.request())
            if (response.code != 429 || attempt++ >= maxRetries) return response
            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 1L
            response.close()
            sleep(retryAfter.coerceIn(1L, 5L) * 1000)
        }
    }

    @Synchronized
    private fun awaitSlot() {
        val wait = lastCallAt + minIntervalMs - System.currentTimeMillis()
        if (wait > 0) sleep(wait)
        lastCallAt = System.currentTimeMillis()
    }
}

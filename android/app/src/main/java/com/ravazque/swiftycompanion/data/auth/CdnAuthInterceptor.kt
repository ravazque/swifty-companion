package com.ravazque.swiftycompanion.data.auth

import okhttp3.Interceptor
import okhttp3.Response

// The intra CDN serves profile photos only with an API token; logos and every other host get no token.
class CdnAuthInterceptor(
    private val tokens: TokenManager,
    private val host: String = CDN_HOST,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != host || !request.url.encodedPath.startsWith(USERS_PATH)) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header(AUTHORIZATION, BEARER + tokens.validToken()).build())
    }

    companion object {
        const val CDN_HOST = "cdn.intra.42.fr"
        private const val USERS_PATH = "/users/"
    }
}

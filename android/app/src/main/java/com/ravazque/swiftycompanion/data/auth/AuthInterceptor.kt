package com.ravazque.swiftycompanion.data.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokens: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(AUTHORIZATION, BEARER + tokens.validToken())
            .build()
        return chain.proceed(request)
    }
}

internal const val AUTHORIZATION = "Authorization"
internal const val BEARER = "Bearer "

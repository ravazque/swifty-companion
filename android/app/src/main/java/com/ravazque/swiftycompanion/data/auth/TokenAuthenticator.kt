package com.ravazque.swiftycompanion.data.auth

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

// OkHttp calls this on a 401: renew once and replay, give up if the replay is rejected too.
class TokenAuthenticator(private val tokens: TokenManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val rejected = response.request.header(AUTHORIZATION)?.removePrefix(BEARER) ?: return null
        val fresh = tokens.renewAfterRejection(rejected)
        return response.request.newBuilder()
            .header(AUTHORIZATION, BEARER + fresh)
            .build()
    }
}

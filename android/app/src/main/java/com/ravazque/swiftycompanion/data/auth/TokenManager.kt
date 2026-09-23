package com.ravazque.swiftycompanion.data.auth

import android.util.Log
import java.io.IOException

class TokenException(val code: Int) : IOException("Token request failed with HTTP $code")

// Single owner of the access token. Both entry points are synchronized so concurrent
// requests never trigger more than one renewal.
class TokenManager(
    private val api: AuthApi,
    private val store: TokenStore,
    private val clientId: String,
    private val clientSecret: String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var token: Token? = null
    private var loaded = false

    val hasCredentials: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    @Synchronized
    fun validToken(): String {
        val current = current()
        val left = current?.let { it.expiresAt - clock() } ?: 0L
        if (current != null && left > EXPIRY_MARGIN_MS) {
            Log.d(TAG, "token reused, ${left / 1000}s left")
            return current.value
        }
        return renew("proactive").value
    }

    @Synchronized
    fun renewAfterRejection(rejected: String): String {
        val current = current()
        if (current != null && current.value != rejected) return current.value
        return renew("after 401").value
    }

    private fun current(): Token? {
        if (!loaded) {
            token = store.load()
            loaded = true
        }
        return token
    }

    private fun renew(reason: String): Token {
        val requestedAt = clock()
        val response = api.token(GRANT_TYPE, clientId, clientSecret).execute()
        val body = response.body()
        if (!response.isSuccessful || body == null) throw TokenException(response.code())
        return Token(body.accessToken, requestedAt + body.expiresIn * 1000).also {
            token = it
            store.save(it)
            Log.i(TAG, "token renewed ($reason), ${body.expiresIn}s left")
        }
    }

    companion object {
        const val EXPIRY_MARGIN_MS = 60_000L
        private const val TAG = "Auth"
        private const val GRANT_TYPE = "client_credentials"
    }
}

package com.ravazque.swiftycompanion.data.auth

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import java.io.IOException

// An IOException because OkHttp only reports IOExceptions thrown inside an interceptor; anything
// else would crash its thread. code is null when the answer was not a token.
class TokenException(val code: Int?) : IOException("Token request failed (HTTP $code)")

enum class RenewalReason { PROACTIVE, AFTER_401 }

data class Renewal(val at: Long, val reason: RenewalReason)

data class TokenInfo(
    val fingerprint: String? = null,
    val expiresAt: Long? = null,
    val apiRequests: Int = 0,
    val tokenRequests: Int = 0,
    val lastRenewal: Renewal? = null,
)

// Single owner of the access token. Every entry point is synchronized so concurrent
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
    private var apiRequests = 0
    private var tokenRequests = 0
    private var lastRenewal: Renewal? = null

    private val _info = MutableStateFlow(TokenInfo())
    val info: StateFlow<TokenInfo> = _info.asStateFlow()

    val hasCredentials: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    @Synchronized
    fun validToken(): String {
        val current = current()
        val left = current?.let { it.expiresAt - clock() } ?: 0L
        if (current != null && left > EXPIRY_MARGIN_MS) {
            Log.d(TAG, "token reused, ${left / 1000}s left")
            return use(current)
        }
        return use(renew(RenewalReason.PROACTIVE))
    }

    @Synchronized
    fun renewAfterRejection(rejected: String): String {
        val current = current()
        if (current != null && current.value != rejected) return use(current)
        return use(renew(RenewalReason.AFTER_401))
    }

    @Synchronized
    fun refreshInfo() {
        current()
        publish()
    }

    // Debug tools: an expired token sends the next request through the proactive renewal, a
    // corrupted one through the 401 path.
    @Synchronized
    fun expireNow() = replace { it.copy(expiresAt = clock()) }

    @Synchronized
    fun corrupt() = replace { it.copy(value = "0".repeat(it.value.length)) }

    private fun current(): Token? {
        if (!loaded) {
            token = store.load()
            loaded = true
        }
        return token
    }

    private fun use(token: Token): String {
        apiRequests++
        publish()
        return token.value
    }

    private fun replace(change: (Token) -> Token) {
        val changed = current()?.let(change) ?: return
        token = changed
        store.save(changed)
        publish()
    }

    private fun renew(reason: RenewalReason): Token {
        val requestedAt = clock()
        tokenRequests++
        val response = try {
            api.token(GRANT_TYPE, clientId, clientSecret).execute()
        } catch (_: SerializationException) {
            throw TokenException(null)
        }
        if (!response.isSuccessful) throw TokenException(response.code())
        val body = response.body() ?: throw TokenException(null)
        return Token(body.accessToken, requestedAt + body.expiresIn * 1000).also {
            token = it
            store.save(it)
            lastRenewal = Renewal(clock(), reason)
            Log.i(TAG, "token renewed (${reason.label}), ${body.expiresIn}s left")
        }
    }

    private fun publish() {
        _info.value = TokenInfo(token?.fingerprint, token?.expiresAt, apiRequests, tokenRequests, lastRenewal)
    }

    companion object {
        const val EXPIRY_MARGIN_MS = 60_000L
        private const val TAG = "SwiftyAuth"
        private const val GRANT_TYPE = "client_credentials"
    }
}

private val RenewalReason.label: String
    get() = when (this) {
        RenewalReason.PROACTIVE -> "proactive"
        RenewalReason.AFTER_401 -> "after 401"
    }

package com.ravazque.swiftycompanion.data.auth

import com.ravazque.swiftycompanion.model.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.HttpException
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

// Bumped whenever the terms of use or the privacy policy change: everyone has to accept them again.
const val TERMS_VERSION = 1

// Registered as the redirect URI of the app on the intra; MainActivity receives it.
const val REDIRECT_URI = "com.ravazque.swiftycompanion://oauth"

// Sign-in with the intra (OAuth2 authorization code with state and PKCE). It only proves the person
// has a 42 account: the user's token reads /v2/me once and is dropped. Searches keep the app token.
class SessionManager(
    private val auth: AuthApi,
    private val me: MeApi,
    private val store: SessionStore,
    private val clientId: String,
    private val clientSecret: String,
    private val authorizeUrl: String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
) {
    private val _signedInAs = MutableStateFlow(store.loadSession()?.takeIf { it.termsVersion == TERMS_VERSION }?.login)
    val signedInAs: StateFlow<String?> = _signedInAs.asStateFlow()

    private val _redirects = Channel<String>(Channel.CONFLATED)
    val redirects: Flow<String> = _redirects.receiveAsFlow()

    fun onRedirect(uri: String) {
        _redirects.trySend(uri)
    }

    fun startLogin(): String {
        if (clientId.isBlank() || clientSecret.isBlank()) throw AppError.MissingCredentials
        val pending = PendingLogin(randomToken(16), randomToken(32), TERMS_VERSION, clock())
        store.savePending(pending)
        return authorizeUrl.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("redirect_uri", REDIRECT_URI)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", "public")
            .addQueryParameter("state", pending.state)
            .addQueryParameter("code_challenge", codeChallenge(pending.verifier))
            .addQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    // Null when the redirect is not an answer to a sign-in in progress (a repeated or foreign one).
    suspend fun completeLogin(redirect: String): String? {
        if (!redirect.startsWith(REDIRECT_URI)) return null
        val pending = store.loadPending() ?: return null
        store.savePending(null)
        val query = queryOf(redirect)
        if (query["state"] != pending.state) throw AppError.LoginFailed
        if (query["error"] == ACCESS_DENIED) throw AppError.LoginCancelled
        val code = query["code"] ?: throw AppError.LoginFailed
        try {
            val token = auth.exchange(GRANT_TYPE, clientId, clientSecret, code, REDIRECT_URI, pending.verifier)
            val login = me.me(BEARER + token.accessToken).login
            store.saveSession(Session(login, pending.termsVersion, pending.acceptedAt))
            _signedInAs.value = login
            return login
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw when (val status = e.code()) {
                429 -> AppError.RateLimited
                in 500..599 -> AppError.Server(status)
                else -> AppError.LoginFailed
            }
        } catch (_: SerializationException) {
            throw AppError.LoginFailed
        } catch (_: IOException) {
            throw AppError.Network
        }
    }

    fun signOut() {
        store.saveSession(null)
        _signedInAs.value = null
    }

    private fun randomToken(bytes: Int): String = base64Url(ByteArray(bytes).also(random::nextBytes))

    private companion object {
        const val GRANT_TYPE = "authorization_code"
        const val ACCESS_DENIED = "access_denied"
    }
}

// PKCE (RFC 7636): the intra gets the hash now and the verifier with the code, so a stolen code is useless.
internal fun codeChallenge(verifier: String): String =
    base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

private fun queryOf(uri: String): Map<String, String> =
    URI(uri).rawQuery.orEmpty().split('&').filter { '=' in it }.associate {
        val (key, value) = it.split('=', limit = 2)
        URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
    }

package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.FakeIntra
import com.ravazque.swiftycompanion.MemorySessionStore
import com.ravazque.swiftycompanion.data.auth.REDIRECT_URI
import com.ravazque.swiftycompanion.data.auth.Session
import com.ravazque.swiftycompanion.data.auth.TERMS_VERSION
import com.ravazque.swiftycompanion.data.auth.codeChallenge
import com.ravazque.swiftycompanion.failureOf
import com.ravazque.swiftycompanion.json
import com.ravazque.swiftycompanion.model.AppError
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

// Sign-in with the intra: authorization code with state and PKCE. The user token only reads /v2/me.
class SessionTest {
    private val intra = FakeIntra()
    private val store = MemorySessionStore()
    private val session = intra.session(store, clock = { 1_000L })

    @After
    fun tearDown() = intra.close()

    private fun stateOf(url: String) = url.toHttpUrl().queryParameter("state")!!

    @Test
    fun codeChallengeFollowsRfc7636() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }

    @Test
    fun startingALoginOpensTheIntraAuthorizePage() {
        val url = session.startLogin().toHttpUrl()
        val pending = store.pending!!

        assertEquals("/oauth/authorize", url.encodedPath)
        assertEquals("id", url.queryParameter("client_id"))
        assertEquals(REDIRECT_URI, url.queryParameter("redirect_uri"))
        assertEquals("code", url.queryParameter("response_type"))
        assertEquals("public", url.queryParameter("scope"))
        assertEquals(pending.state, url.queryParameter("state"))
        assertEquals(codeChallenge(pending.verifier), url.queryParameter("code_challenge"))
        assertEquals("S256", url.queryParameter("code_challenge_method"))
        assertEquals(TERMS_VERSION, pending.termsVersion)
        assertEquals(1_000L, pending.acceptedAt)
        assertEquals(0, intra.server.requestCount)
    }

    @Test
    fun aCompletedLoginKeepsOnlyTheLogin() = runTest {
        val state = stateOf(session.startLogin())
        val verifier = store.pending!!.verifier

        assertEquals("ravazque", session.completeLogin("$REDIRECT_URI?code=abc&state=$state"))

        val form = intra.exchangeForms.single()
        assertEquals("abc", form["code"])
        assertEquals(REDIRECT_URI, form["redirect_uri"])
        assertEquals(verifier, form["code_verifier"])
        assertEquals("id", form["client_id"])
        assertEquals("Bearer user-token", intra.meAuthHeaders.single())
        assertEquals(Session("ravazque", TERMS_VERSION, 1_000L), store.session)
        assertNull(store.pending)
        assertEquals("ravazque", session.signedInAs.value)
        assertEquals(0, intra.tokenRequests.get())
    }

    @Test
    fun aRedirectWithAnotherStateIsRejectedWithoutAnyRequest() = runTest {
        session.startLogin()

        assertEquals(AppError.LoginFailed, failureOf { session.completeLogin("$REDIRECT_URI?code=abc&state=forged") })
        assertNull(store.pending)
        assertNull(session.signedInAs.value)
        assertEquals(0, intra.server.requestCount)
    }

    @Test
    fun deniedAccessIsACancelledLogin() = runTest {
        val state = stateOf(session.startLogin())

        assertEquals(AppError.LoginCancelled, failureOf { session.completeLogin("$REDIRECT_URI?error=access_denied&state=$state") })
        assertNull(store.pending)
        assertEquals(0, intra.server.requestCount)
    }

    @Test
    fun redirectsWithoutAPendingLoginAreIgnored() = runTest {
        assertNull(session.completeLogin("$REDIRECT_URI?code=abc&state=x"))

        val state = stateOf(session.startLogin())
        assertNull(session.completeLogin("https://example.com/oauth?code=abc&state=$state"))
        assertEquals(0, intra.server.requestCount)
    }

    @Test
    fun aRejectedCodeFailsTheLogin() = runTest {
        intra.exchangeResponse = { json(401, """{"error": "invalid_grant"}""") }
        val state = stateOf(session.startLogin())

        assertEquals(AppError.LoginFailed, failureOf { session.completeLogin("$REDIRECT_URI?code=abc&state=$state") })
        assertNull(store.session)
        assertEquals(0, intra.meAuthHeaders.size)
    }

    @Test
    fun signOutForgetsTheSessionAndNewTermsAskAgain() {
        store.session = Session("old", TERMS_VERSION - 1, 0L)
        assertNull(intra.session(store).signedInAs.value)

        store.session = Session("me", TERMS_VERSION, 0L)
        val current = intra.session(store)
        assertEquals("me", current.signedInAs.value)

        current.signOut()
        assertNull(current.signedInAs.value)
        assertNull(store.session)
    }

    @Test
    fun signingInNeedsTheAppCredentials() {
        assertThrows(AppError.MissingCredentials::class.java) { intra.session(clientId = "").startLogin() }
    }
}

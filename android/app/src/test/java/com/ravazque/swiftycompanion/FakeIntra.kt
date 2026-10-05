package com.ravazque.swiftycompanion

import com.ravazque.swiftycompanion.data.ProjectViewStore
import com.ravazque.swiftycompanion.data.UserRepository
import com.ravazque.swiftycompanion.data.VisibilityStore
import com.ravazque.swiftycompanion.data.auth.PendingLogin
import com.ravazque.swiftycompanion.data.auth.Session
import com.ravazque.swiftycompanion.data.auth.SessionManager
import com.ravazque.swiftycompanion.data.auth.SessionStore
import com.ravazque.swiftycompanion.data.auth.Token
import com.ravazque.swiftycompanion.data.auth.TokenManager
import com.ravazque.swiftycompanion.data.auth.TokenStore
import com.ravazque.swiftycompanion.data.net.ApiClient
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.ProfileGroup
import com.ravazque.swiftycompanion.model.ProjectView
import com.ravazque.swiftycompanion.model.Visibility
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.io.Closeable
import java.net.URLDecoder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

// The smallest profile the app shows: a student whose main cursus started in the past.
const val MINIMAL_USER =
    """{"id": 1, "login": "jdoe", "cursus_users": [{"begin_at": "2020-01-01T00:00:00.000Z", "cursus": {"id": 21, "name": "42cursus", "slug": "42cursus"}}]}"""

// Local stand-in for the API: token endpoint (app token and sign-in code exchange), /v2/me, user
// endpoint and coalitions, with scriptable answers.
class FakeIntra : Closeable {
    val server = MockWebServer()
    val tokenRequests = AtomicInteger()
    val userAuthHeaders = CopyOnWriteArrayList<String?>()
    val exchangeForms = CopyOnWriteArrayList<Map<String, String>>()
    val meAuthHeaders = CopyOnWriteArrayList<String?>()
    val sleeps = CopyOnWriteArrayList<Long>()

    var tokenResponse: (Int) -> MockResponse = { n -> json(200, """{"access_token": "t$n", "expires_in": 7200}""") }
    var exchangeResponse: () -> MockResponse = { json(200, """{"access_token": "user-token", "expires_in": 7200, "refresh_token": "r"}""") }
    var me = """{"id": 9, "login": "ravazque"}"""
    var userResponse: (RecordedRequest, Int) -> MockResponse = { _, _ -> json(200, MINIMAL_USER) }
    var coalitions = "[]"
    var coalitionsUsers = "[]"

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/oauth/token" -> {
                        val form = formOf(request.body?.utf8().orEmpty())
                        if (form["grant_type"] == "authorization_code") {
                            exchangeForms += form
                            exchangeResponse()
                        } else {
                            tokenResponse(tokenRequests.incrementAndGet())
                        }
                    }
                    path == "/v2/me" -> {
                        meAuthHeaders += request.headers["Authorization"]
                        json(200, me)
                    }
                    path.endsWith("/coalitions") -> json(200, coalitions)
                    path.endsWith("/coalitions_users") -> json(200, coalitionsUsers)
                    else -> {
                        userAuthHeaders += request.headers["Authorization"]
                        userResponse(request, userAuthHeaders.size)
                    }
                }
            }
        }
        server.start()
    }

    // Token manager of the last repository built.
    lateinit var tokens: TokenManager
        private set

    fun repository(
        store: TokenStore = MemoryStore(),
        clock: () -> Long = { 0L },
        clientId: String = "id",
        visibility: Visibility = Visibility(),
    ): UserRepository {
        val client = ApiClient(server.url("/").toString(), clientId, "secret", store, clock, sleep = { sleeps += it })
        tokens = client.tokens
        return UserRepository(client.api, client.tokens, MemoryVisibilityStore(visibility))
    }

    fun session(store: SessionStore = MemorySessionStore(), clientId: String = "id", clock: () -> Long = { 0L }): SessionManager {
        val client = ApiClient(server.url("/").toString(), clientId, "secret", MemoryStore(), clock, sleep = { sleeps += it })
        return SessionManager(client.auth, client.me, store, clientId, "secret", server.url("/oauth/authorize").toString(), clock)
    }

    override fun close() = server.close()
}

class MemorySessionStore(var session: Session? = null, var pending: PendingLogin? = null) : SessionStore {
    override fun loadSession() = session
    override fun saveSession(session: Session?) {
        this.session = session
    }
    override fun loadPending() = pending
    override fun savePending(pending: PendingLogin?) {
        this.pending = pending
    }
}

fun formOf(body: String): Map<String, String> = body.split('&').filter { '=' in it }.associate {
    val (key, value) = it.split('=', limit = 2)
    URLDecoder.decode(key, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
}

class MemoryVisibilityStore(var visibility: Visibility = Visibility()) : VisibilityStore {
    override fun load() = visibility
    override fun save(visibility: Visibility) {
        this.visibility = visibility
    }
}

class MemoryProjectViewStore : ProjectViewStore {
    private val views = mutableMapOf<ProfileGroup, ProjectView>()
    override fun load(group: ProfileGroup) = views[group] ?: ProjectView()
    override fun save(group: ProfileGroup, view: ProjectView) {
        views[group] = view
    }
}

class MemoryStore(var token: Token? = null) : TokenStore {
    override fun load() = token
    override fun save(token: Token) {
        this.token = token
    }
}

fun json(code: Int, body: String, vararg headers: Pair<String, String>): MockResponse =
    MockResponse.Builder()
        .code(code)
        .body(body)
        .addHeader("Content-Type", "application/json")
        .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
        .build()

suspend fun failureOf(block: suspend () -> Unit): AppError {
    try {
        block()
    } catch (e: AppError) {
        return e
    }
    throw AssertionError("expected an AppError")
}

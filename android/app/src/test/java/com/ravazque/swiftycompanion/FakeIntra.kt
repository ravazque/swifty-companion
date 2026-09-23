package com.ravazque.swiftycompanion

import com.ravazque.swiftycompanion.data.UserRepository
import com.ravazque.swiftycompanion.data.auth.Token
import com.ravazque.swiftycompanion.data.auth.TokenStore
import com.ravazque.swiftycompanion.data.net.ApiClient
import com.ravazque.swiftycompanion.model.AppError
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

const val MINIMAL_USER = """{"id": 1, "login": "jdoe"}"""

// Local stand-in for the API: token endpoint, user endpoint and coalitions, with scriptable answers.
class FakeIntra : Closeable {
    val server = MockWebServer()
    val tokenRequests = AtomicInteger()
    val userAuthHeaders = CopyOnWriteArrayList<String?>()
    val sleeps = CopyOnWriteArrayList<Long>()

    var tokenResponse: (Int) -> MockResponse = { n -> json(200, """{"access_token": "t$n", "expires_in": 7200}""") }
    var userResponse: (RecordedRequest, Int) -> MockResponse = { _, _ -> json(200, MINIMAL_USER) }

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/oauth/token" -> tokenResponse(tokenRequests.incrementAndGet())
                    path.endsWith("/coalitions") -> json(200, "[]")
                    else -> {
                        userAuthHeaders += request.headers["Authorization"]
                        userResponse(request, userAuthHeaders.size)
                    }
                }
            }
        }
        server.start()
    }

    fun repository(
        store: TokenStore = MemoryStore(),
        clock: () -> Long = { 0L },
        clientId: String = "id",
    ): UserRepository {
        val client = ApiClient(server.url("/").toString(), clientId, "secret", store, clock, sleep = { sleeps += it })
        return UserRepository(client.api, client.tokens)
    }

    override fun close() = server.close()
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

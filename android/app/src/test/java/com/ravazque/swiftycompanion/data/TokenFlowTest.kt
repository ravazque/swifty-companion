package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.FakeIntra
import com.ravazque.swiftycompanion.MINIMAL_USER
import com.ravazque.swiftycompanion.MemoryStore
import com.ravazque.swiftycompanion.data.auth.TokenManager
import com.ravazque.swiftycompanion.failureOf
import com.ravazque.swiftycompanion.json
import com.ravazque.swiftycompanion.model.AppError
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenFlowTest {
    private val intra = FakeIntra()

    @After
    fun tearDown() = intra.close()

    @Test
    fun reusesOneTokenAcrossRequests() = runTest {
        val repository = intra.repository()
        repository.fetch("jdoe")
        repository.fetch("jdoe")

        assertEquals(1, intra.tokenRequests.get())
        assertEquals(listOf("Bearer t1", "Bearer t1"), intra.userAuthHeaders)
    }

    @Test
    fun reusesPersistedTokenAfterRestart() = runTest {
        val store = MemoryStore()
        intra.repository(store).fetch("jdoe")
        intra.repository(store).fetch("jdoe")

        assertEquals(1, intra.tokenRequests.get())
    }

    @Test
    fun renewsTokenBeforeItExpires() = runTest {
        var now = 0L
        val repository = intra.repository(clock = { now })
        repository.fetch("jdoe")
        now = 7_200_000L - TokenManager.EXPIRY_MARGIN_MS
        repository.fetch("jdoe")

        assertEquals(2, intra.tokenRequests.get())
        assertEquals(listOf("Bearer t1", "Bearer t2"), intra.userAuthHeaders)
    }

    @Test
    fun renewsAndReplaysOnceAfter401() = runTest {
        intra.userResponse = { request, _ ->
            if (request.headers["Authorization"] == "Bearer t1") json(401, "{}") else json(200, MINIMAL_USER)
        }

        val profile = intra.repository().fetch("jdoe")

        assertEquals("jdoe", profile.login)
        assertEquals(2, intra.tokenRequests.get())
        assertEquals(listOf("Bearer t1", "Bearer t2"), intra.userAuthHeaders)
    }

    @Test
    fun givesUpWhenTheReplayIsRejectedToo() = runTest {
        intra.userResponse = { _, _ -> json(401, "{}") }

        assertEquals(AppError.Unauthorized, failureOf { intra.repository().fetch("jdoe") })
        assertEquals(2, intra.userAuthHeaders.size)
    }

    @Test
    fun rejectedCredentialsAreUnauthorized() = runTest {
        intra.tokenResponse = { json(401, """{"error": "invalid_client"}""") }

        assertEquals(AppError.Unauthorized, failureOf { intra.repository().fetch("jdoe") })
        assertEquals(0, intra.userAuthHeaders.size)
    }

    @Test
    fun missingCredentialsNeverReachTheNetwork() = runTest {
        assertEquals(AppError.MissingCredentials, failureOf { intra.repository(clientId = "").fetch("jdoe") })
        assertEquals(0, intra.server.requestCount)
    }
}

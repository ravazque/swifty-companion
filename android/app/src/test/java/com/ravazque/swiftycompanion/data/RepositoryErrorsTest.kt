package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.FakeIntra
import com.ravazque.swiftycompanion.MINIMAL_USER
import com.ravazque.swiftycompanion.failureOf
import com.ravazque.swiftycompanion.json
import com.ravazque.swiftycompanion.model.AppError
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryErrorsTest {
    private val intra = FakeIntra()

    @After
    fun tearDown() = intra.close()

    @Test
    fun unknownLoginIsNotFound() = runTest {
        intra.userResponse = { _, _ -> json(404, "{}") }
        assertEquals(AppError.NotFound("ghost"), failureOf { intra.repository().fetch("ghost") })
    }

    @Test
    fun rateLimitIsRetriedAfterTheRequestedDelay() = runTest {
        intra.userResponse = { _, n -> if (n == 1) json(429, "{}", "Retry-After" to "2") else json(200, MINIMAL_USER) }

        assertEquals("jdoe", intra.repository().fetch("jdoe").login)
        assertTrue(2_000L in intra.sleeps)
    }

    @Test
    fun persistentRateLimitIsReported() = runTest {
        intra.userResponse = { _, _ -> json(429, "{}", "Retry-After" to "1") }
        assertEquals(AppError.RateLimited, failureOf { intra.repository().fetch("jdoe") })
        assertEquals(3, intra.userAuthHeaders.size)
    }

    @Test
    fun serverErrorKeepsTheStatusCode() = runTest {
        intra.userResponse = { _, _ -> json(503, "{}") }
        assertEquals(AppError.Server(503), failureOf { intra.repository().fetch("jdoe") })
    }

    @Test
    fun malformedBodyIsUnexpectedResponse() = runTest {
        intra.userResponse = { _, _ -> json(200, "<html>not json</html>") }
        assertEquals(AppError.UnexpectedResponse, failureOf { intra.repository().fetch("jdoe") })
    }

    @Test
    fun unreachableServerIsNetworkError() = runTest {
        val repository = intra.repository()
        intra.close()
        assertEquals(AppError.Network, failureOf { repository.fetch("jdoe") })
    }

    @Test
    fun successfulFetchIsCached() = runTest {
        val repository = intra.repository()
        repository.fetch("jdoe")
        assertEquals("jdoe", repository.cached("jdoe")?.login)
    }
}

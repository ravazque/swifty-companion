package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.FakeIntra
import com.ravazque.swiftycompanion.MINIMAL_USER
import com.ravazque.swiftycompanion.failureOf
import com.ravazque.swiftycompanion.json
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.Coalition
import com.ravazque.swiftycompanion.model.ProfileKind
import com.ravazque.swiftycompanion.model.Visibility
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun staffAndPiscinersAreHiddenWithoutAskingForTheirCoalition() = runTest {
        intra.userResponse = { _, _ -> json(200, """{"id": 2, "login": "boss", "staff?": true}""") }
        assertEquals(AppError.Hidden("boss", ProfileKind.STAFF), failureOf { intra.repository().fetch("boss") })

        intra.userResponse = { _, _ -> json(200, """{"id": 3, "login": "newbie"}""") }
        val repository = intra.repository()
        assertEquals(AppError.Hidden("newbie", ProfileKind.PISCINER), failureOf { repository.fetch("newbie") })
        assertNull(repository.cached("newbie"))
        assertEquals(2, intra.server.requestCount - intra.tokenRequests.get())
    }

    @Test
    fun staffAndBlackholedProfilesFollowTheSearchOptions() = runTest {
        intra.userResponse = { request, _ ->
            if (request.url.encodedPath.endsWith("/boss")) json(200, STAFF_USER) else json(200, BLACKHOLED_USER)
        }

        assertEquals(AppError.Hidden("gone", ProfileKind.BLACKHOLED), failureOf { intra.repository().fetch("gone") })
        assertEquals(ProfileKind.BLACKHOLED, intra.repository(visibility = Visibility(blackholed = true)).fetch("gone").kind)
        assertEquals(ProfileKind.STAFF, intra.repository(visibility = Visibility(staff = true)).fetch("boss").kind)

        val both = intra.repository(visibility = Visibility(staff = true, blackholed = true))
        assertEquals(ProfileKind.STAFF, both.fetch("boss").kind)
        assertEquals(ProfileKind.BLACKHOLED, both.fetch("gone").kind)
    }

    @Test
    fun theMainCursusCoalitionIsAddedToTheProfile() = runTest {
        intra.coalitions = """[{"id": 555, "name": "Corvus", "slug": "corvus", "color": "#d087ab"},
            {"id": 398, "name": "Ignisaria", "slug": "ignisaria", "color": "#C2301D"}]"""
        assertEquals(Coalition("Ignisaria", "#C2301D", null), intra.repository().fetch("jdoe").coalition)
    }

    @Test
    fun failedCoalitionCallStillShowsTheProfile() = runTest {
        intra.coalitions = "oops"
        val profile = intra.repository().fetch("jdoe")
        assertEquals("jdoe", profile.login)
        assertEquals(null, profile.coalition)
    }

    @Test
    fun successfulFetchIsCached() = runTest {
        val repository = intra.repository()
        repository.fetch("jdoe")
        assertEquals("jdoe", repository.cached("jdoe")?.login)
    }
}

private const val STAFF_USER = """{"id": 2, "login": "boss", "staff?": true}"""

private const val BLACKHOLED_USER = """{"id": 4, "login": "gone", "cursus_users": [{"begin_at": "2026-05-18T07:42:00.000Z",
    "end_at": "2026-07-03T22:01:04.781Z", "blackholed_at": "2026-08-05T07:42:00.000Z", "grade": "Cadet",
    "cursus": {"id": 21, "name": "42cursus", "slug": "42cursus"}}]}"""

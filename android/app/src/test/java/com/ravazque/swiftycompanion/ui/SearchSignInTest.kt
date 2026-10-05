package com.ravazque.swiftycompanion.ui

import androidx.lifecycle.SavedStateHandle
import com.ravazque.swiftycompanion.FakeIntra
import com.ravazque.swiftycompanion.MemorySessionStore
import com.ravazque.swiftycompanion.data.auth.REDIRECT_URI
import com.ravazque.swiftycompanion.data.auth.Session
import com.ravazque.swiftycompanion.data.auth.TERMS_VERSION
import com.ravazque.swiftycompanion.json
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.ui.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Searching needs a signed-in 42 account; the consent comes first and the search resumes after the sign-in.
@OptIn(ExperimentalCoroutinesApi::class)
class SearchSignInTest {
    private val intra = FakeIntra()
    private val store = MemorySessionStore()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        intra.close()
    }

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        SearchViewModel(saved, intra.repository(), intra.session(store)).apply { onQueryChange("jdoe") }

    private fun <T> await(block: suspend () -> T): T = runBlocking { withTimeout(5_000) { block() } }

    @Test
    fun searchingWithoutASessionAsksForConsentFirst() {
        val viewModel = viewModel().apply { search() }

        assertTrue(viewModel.state.value.consentOpen)
        assertEquals(0, intra.server.requestCount)
    }

    @Test
    fun anInvalidLoginIsReportedBeforeAskingToSignIn() {
        val viewModel = viewModel().apply {
            onQueryChange("  ")
            search()
        }

        assertEquals(AppError.EmptyLogin, viewModel.state.value.error)
        assertFalse(viewModel.state.value.consentOpen)
    }

    @Test
    fun theSearchRunsOnItsOwnAfterSigningIn() {
        val viewModel = viewModel().apply {
            search()
            signIn()
        }
        val state = await { viewModel.openLogin.first() }.toHttpUrl().queryParameter("state")
        assertFalse(viewModel.state.value.consentOpen)

        viewModel.completeLogin("$REDIRECT_URI?code=abc&state=$state")

        assertEquals("jdoe", await { viewModel.found.first() })
        assertEquals("ravazque", viewModel.account.value)
    }

    @Test
    fun withASessionTheSearchGoesStraightToTheProfile() {
        store.session = Session("me", TERMS_VERSION, 0L)
        val viewModel = viewModel().apply { search() }

        assertEquals("jdoe", await { viewModel.found.first() })
        assertFalse(viewModel.state.value.consentOpen)
    }

    @Test
    fun aFailedSignInShowsItsErrorAndDropsThePendingSearch() {
        intra.exchangeResponse = { json(401, """{"error": "invalid_grant"}""") }
        val viewModel = viewModel().apply {
            search()
            signIn()
        }
        val state = await { viewModel.openLogin.first() }.toHttpUrl().queryParameter("state")

        viewModel.completeLogin("$REDIRECT_URI?code=abc&state=$state")
        await { while (viewModel.state.value.loading) kotlinx.coroutines.delay(10) }

        assertEquals(AppError.LoginFailed, viewModel.state.value.error)
        assertNull(viewModel.account.value)
        assertEquals(0, intra.userAuthHeaders.size)
    }

    @Test
    fun closingTheConsentKeepsTheSearchScreenAsItWas() {
        val viewModel = viewModel().apply {
            search()
            dismissConsent()
        }

        assertFalse(viewModel.state.value.consentOpen)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun aPendingSearchSurvivesProcessDeathDuringTheSignIn() {
        val saved = SavedStateHandle()
        viewModel(saved).apply {
            search()
            signIn()
        }
        val state = store.pending!!.state

        val restored = SearchViewModel(SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }), intra.repository(), intra.session(store))
        restored.completeLogin("$REDIRECT_URI?code=abc&state=$state")

        assertEquals("jdoe", await { restored.found.first() })
    }
}

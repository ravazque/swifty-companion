package com.ravazque.swiftycompanion.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravazque.swiftycompanion.SwiftyApp
import com.ravazque.swiftycompanion.data.UserRepository
import com.ravazque.swiftycompanion.data.auth.SessionManager
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.Visibility
import com.ravazque.swiftycompanion.model.normalizeLogin
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val error: AppError? = null,
    val visibility: Visibility = Visibility(),
    val consentOpen: Boolean = false,
)

// Searching needs a signed-in 42 account. Without one, a valid search first asks to accept the terms,
// then opens the intra sign-in; when the browser comes back with the code, the search runs on its own.
class SearchViewModel(
    private val savedState: SavedStateHandle,
    private val repository: UserRepository,
    private val session: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(
        SearchUiState(
            query = savedState[QUERY_KEY] ?: "",
            visibility = repository.visibility,
            consentOpen = savedState[CONSENT_KEY] ?: false,
        ),
    )
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val account: StateFlow<String?> = session.signedInAs
    val redirects: Flow<String> = session.redirects

    // One-shot events: a Channel is consumed once, so a rotation does not navigate or open the browser again.
    private val _found = Channel<String>(Channel.BUFFERED)
    val found: Flow<String> = _found.receiveAsFlow()

    private val _openLogin = Channel<String>(Channel.BUFFERED)
    val openLogin: Flow<String> = _openLogin.receiveAsFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        savedState[QUERY_KEY] = query
        _state.update { it.copy(query = query, error = null) }
    }

    fun showStaff(show: Boolean) = changeVisibility { it.copy(staff = show) }

    fun showBlackholed(show: Boolean) = changeVisibility { it.copy(blackholed = show) }

    fun showFrozen(show: Boolean) = changeVisibility { it.copy(frozen = show) }

    private fun changeVisibility(change: (Visibility) -> Visibility) {
        val visibility = change(_state.value.visibility)
        repository.visibility = visibility
        _state.update { it.copy(visibility = visibility, error = null) }
    }

    fun search() {
        val login = try {
            normalizeLogin(_state.value.query)
        } catch (e: AppError) {
            _state.update { it.copy(error = e) }
            return
        }
        if (session.signedInAs.value == null) {
            savedState[PENDING_KEY] = true
            setConsentOpen(true)
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { find(login) }
    }

    fun dismissConsent() {
        savedState[PENDING_KEY] = false
        setConsentOpen(false)
    }

    // The terms were accepted: the browser opens the intra sign-in page.
    fun signIn() {
        setConsentOpen(false)
        try {
            _openLogin.trySend(session.startLogin())
        } catch (e: AppError) {
            failSignIn(e)
        }
    }

    fun browserMissing() = failSignIn(AppError.NoBrowser)

    fun completeLogin(redirect: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val signedIn = session.completeLogin(redirect) != null
                if (signedIn && savedState.get<Boolean>(PENDING_KEY) == true) {
                    savedState[PENDING_KEY] = false
                    find(normalizeLogin(_state.value.query))
                } else {
                    _state.update { it.copy(loading = false) }
                }
            } catch (e: AppError) {
                failSignIn(e)
            }
        }
    }

    fun signOut() = session.signOut()

    private suspend fun find(login: String) {
        _state.update { it.copy(loading = true, error = null) }
        try {
            repository.fetch(login)
            _state.update { it.copy(loading = false) }
            _found.send(login)
        } catch (e: AppError) {
            _state.update { it.copy(loading = false, error = e) }
        }
    }

    private fun failSignIn(error: AppError) {
        savedState[PENDING_KEY] = false
        _state.update { it.copy(loading = false, error = error) }
    }

    private fun setConsentOpen(open: Boolean) {
        savedState[CONSENT_KEY] = open
        _state.update { it.copy(consentOpen = open, error = null) }
    }

    companion object {
        private const val QUERY_KEY = "query"
        private const val PENDING_KEY = "pendingSearch"
        private const val CONSENT_KEY = "consentOpen"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SwiftyApp).container
                SearchViewModel(createSavedStateHandle(), container.repository, container.session)
            }
        }
    }
}

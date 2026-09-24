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
import com.ravazque.swiftycompanion.model.AppError
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
)

class SearchViewModel(
    private val savedState: SavedStateHandle,
    private val repository: UserRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState(query = savedState[QUERY_KEY] ?: ""))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    // One-shot event: a Channel is consumed once, so a rotation does not navigate again.
    private val _found = Channel<String>(Channel.BUFFERED)
    val found: Flow<String> = _found.receiveAsFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        savedState[QUERY_KEY] = query
        _state.update { it.copy(query = query, error = null) }
    }

    fun search() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val login = normalizeLogin(_state.value.query)
                repository.fetch(login)
                _state.update { it.copy(loading = false) }
                _found.send(login)
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    companion object {
        private const val QUERY_KEY = "query"

        val Factory = viewModelFactory {
            initializer {
                SearchViewModel(createSavedStateHandle(), (this[APPLICATION_KEY] as SwiftyApp).container.repository)
            }
        }
    }
}

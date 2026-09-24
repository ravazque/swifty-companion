package com.ravazque.swiftycompanion.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.ravazque.swiftycompanion.SwiftyApp
import com.ravazque.swiftycompanion.data.UserRepository
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.ui.ProfileDestination
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: Profile? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    val selectedCursusId: Int? = null,
)

class ProfileViewModel(
    val login: String,
    private val savedState: SavedStateHandle,
    private val repository: UserRepository,
) : ViewModel() {
    // The search screen already fetched this login; the network is only hit again on refresh
    // or when the process was killed and the in-memory cache is gone.
    private val _state = MutableStateFlow(
        ProfileUiState(profile = repository.cached(login), selectedCursusId = savedState[CURSUS_KEY]),
    )
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        if (_state.value.profile == null) load()
    }

    fun selectCursus(id: Int) {
        savedState[CURSUS_KEY] = id
        _state.update { it.copy(selectedCursusId = id) }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val profile = repository.fetch(login)
                _state.update { it.copy(profile = profile, loading = false) }
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    companion object {
        private const val CURSUS_KEY = "selectedCursusId"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SwiftyApp
                val savedState = createSavedStateHandle()
                ProfileViewModel(
                    login = savedState.toRoute<ProfileDestination>().login,
                    savedState = savedState,
                    repository = app.container.repository,
                )
            }
        }
    }
}

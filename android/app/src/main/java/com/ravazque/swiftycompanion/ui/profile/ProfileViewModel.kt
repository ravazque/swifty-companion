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
import com.ravazque.swiftycompanion.data.ProjectViewStore
import com.ravazque.swiftycompanion.data.UserRepository
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.ProjectSort
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.model.ProjectView
import com.ravazque.swiftycompanion.model.group
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
    val projectView: ProjectView = ProjectView(),
    val cardFlipped: Boolean = false,
)

class ProfileViewModel(
    val login: String,
    private val savedState: SavedStateHandle,
    private val repository: UserRepository,
    private val projectViews: ProjectViewStore,
) : ViewModel() {
    // The search screen already fetched this login; the network is only hit again on refresh
    // or when the process was killed and the in-memory cache is gone.
    private val _state = MutableStateFlow(
        ProfileUiState(selectedCursusId = savedState[CURSUS_KEY], cardFlipped = savedState[FLIPPED_KEY] ?: false)
            .withProfile(repository.cached(login)),
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

    fun selectProjectFilter(status: ProjectStatus?) = changeProjectView { it.copy(filter = status) }

    fun selectProjectSort(sort: ProjectSort) = changeProjectView { it.copy(sort = sort) }

    // Saved for the profile's group, so it also survives process death and app restarts.
    private fun changeProjectView(change: (ProjectView) -> ProjectView) {
        val view = change(_state.value.projectView)
        _state.value.profile?.let { projectViews.save(it.kind.group, view) }
        _state.update { it.copy(projectView = view) }
    }

    fun flipCard() {
        val flipped = !_state.value.cardFlipped
        savedState[FLIPPED_KEY] = flipped
        _state.update { it.copy(cardFlipped = flipped) }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val profile = repository.fetch(login)
                _state.update { it.withProfile(profile).copy(loading = false) }
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    // The project filter and sort come with the profile: they belong to its group.
    private fun ProfileUiState.withProfile(profile: Profile?): ProfileUiState =
        if (profile == null) this else copy(profile = profile, projectView = projectViews.load(profile.kind.group))

    companion object {
        private const val CURSUS_KEY = "selectedCursusId"
        private const val FLIPPED_KEY = "cardFlipped"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SwiftyApp
                val savedState = createSavedStateHandle()
                ProfileViewModel(
                    login = savedState.toRoute<ProfileDestination>().login,
                    savedState = savedState,
                    repository = app.container.repository,
                    projectViews = app.container.projectViews,
                )
            }
        }
    }
}

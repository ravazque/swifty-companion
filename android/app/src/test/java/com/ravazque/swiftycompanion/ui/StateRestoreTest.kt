package com.ravazque.swiftycompanion.ui

import androidx.lifecycle.SavedStateHandle
import com.ravazque.swiftycompanion.FakeIntra
import com.ravazque.swiftycompanion.MINIMAL_USER
import com.ravazque.swiftycompanion.MemoryProjectViewStore
import com.ravazque.swiftycompanion.json
import com.ravazque.swiftycompanion.model.ProjectSort
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.model.ProjectView
import com.ravazque.swiftycompanion.model.Visibility
import com.ravazque.swiftycompanion.ui.profile.ProfileViewModel
import com.ravazque.swiftycompanion.ui.search.SearchViewModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

// A killed process only keeps what was written to the SavedStateHandle; the ViewModel is rebuilt from it.
class StateRestoreTest {
    private val intra = FakeIntra()

    @After
    fun tearDown() = intra.close()

    private fun SavedStateHandle.afterProcessDeath() = SavedStateHandle(keys().associateWith { get<Any?>(it) })

    @Test
    fun searchQuerySurvivesProcessDeath() {
        val saved = SavedStateHandle()
        SearchViewModel(saved, intra.repository(), intra.session()).onQueryChange("jdoe")

        val restored = SearchViewModel(saved.afterProcessDeath(), intra.repository(), intra.session())

        assertEquals("jdoe", restored.state.value.query)
    }

    @Test
    fun searchOptionsAreKeptForTheNextLaunch() {
        val repository = intra.repository()
        SearchViewModel(SavedStateHandle(), repository, intra.session()).apply {
            showStaff(true)
            showBlackholed(true)
            showFrozen(true)
            showStaff(false)
        }

        val restored = SearchViewModel(SavedStateHandle(), repository, intra.session()).state.value

        assertEquals(Visibility(staff = false, blackholed = true, frozen = true), restored.visibility)
    }

    @Test
    fun selectedCursusSurvivesProcessDeath() = runTest {
        val repository = intra.repository().apply { fetch("jdoe") }
        val saved = SavedStateHandle()
        ProfileViewModel("jdoe", saved, repository, MemoryProjectViewStore()).selectCursus(9)

        val restored = ProfileViewModel("jdoe", saved.afterProcessDeath(), repository, MemoryProjectViewStore())

        assertEquals(9, restored.state.value.selectedCursusId)
    }

    // Kept in the settings store, not in the SavedStateHandle: it outlives the screen, the process and the app.
    @Test
    fun projectViewIsSharedWithinEachProfileGroup() = runTest {
        intra.userResponse = { request, _ -> json(200, USERS.getValue(request.url.encodedPath.substringAfterLast('/'))) }
        val repository = intra.repository(visibility = Visibility(staff = true, frozen = true)).apply { USERS.keys.forEach { fetch(it) } }
        val views = MemoryProjectViewStore()
        fun open(login: String) = ProfileViewModel(login, SavedStateHandle(), repository, views)

        open("jdoe").apply {
            selectProjectFilter(ProjectStatus.PASSED)
            selectProjectSort(ProjectSort.GRADE)
        }
        open("tran").selectProjectSort(ProjectSort.NAME)

        assertEquals(ProjectView(ProjectStatus.PASSED, ProjectSort.GRADE), open("amy").state.value.projectView)
        assertEquals(ProjectView(sort = ProjectSort.NAME), open("old").state.value.projectView)
        assertEquals(ProjectView(), open("boss").state.value.projectView)
        assertEquals(ProjectView(), open("away").state.value.projectView)
    }

    @Test
    fun cardSideSurvivesProcessDeath() = runTest {
        val repository = intra.repository().apply { fetch("jdoe") }
        val saved = SavedStateHandle()
        val viewModel = ProfileViewModel("jdoe", saved, repository, MemoryProjectViewStore()).apply { flipCard() }
        assertEquals(true, viewModel.state.value.cardFlipped)

        val restored = ProfileViewModel("jdoe", saved.afterProcessDeath(), repository, MemoryProjectViewStore())

        assertEquals(true, restored.state.value.cardFlipped)
        restored.flipCard()
        assertEquals(false, restored.state.value.cardFlipped)
    }
}

// Two students, a transcender, an alumni, a staff member and a student on freeze.
private val USERS = mapOf(
    "jdoe" to MINIMAL_USER,
    "amy" to """{"id": 2, "login": "amy", "cursus_users": [{"begin_at": "2020-01-01T00:00:00.000Z",
        "cursus": {"id": 21, "name": "42cursus", "slug": "42cursus"}}]}""",
    "tran" to """{"id": 3, "login": "tran", "cursus_users": [{"begin_at": "2020-01-01T00:00:00.000Z", "grade": "Transcender",
        "cursus": {"id": 21, "name": "42cursus", "slug": "42cursus"}}]}""",
    "old" to """{"id": 4, "login": "old", "alumni?": true}""",
    "boss" to """{"id": 5, "login": "boss", "staff?": true}""",
    "away" to """{"id": 6, "login": "away", "active?": false, "cursus_users": [{"begin_at": "2020-01-01T00:00:00.000Z",
        "cursus": {"id": 21, "name": "42cursus", "slug": "42cursus"}}]}""",
)

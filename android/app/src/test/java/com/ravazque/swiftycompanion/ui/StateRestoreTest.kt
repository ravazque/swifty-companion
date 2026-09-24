package com.ravazque.swiftycompanion.ui

import androidx.lifecycle.SavedStateHandle
import com.ravazque.swiftycompanion.FakeIntra
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
        SearchViewModel(saved, intra.repository()).onQueryChange("jdoe")

        val restored = SearchViewModel(saved.afterProcessDeath(), intra.repository())

        assertEquals("jdoe", restored.state.value.query)
    }

    @Test
    fun selectedCursusSurvivesProcessDeath() = runTest {
        val repository = intra.repository().apply { fetch("jdoe") }
        val saved = SavedStateHandle()
        ProfileViewModel("jdoe", saved, repository).selectCursus(9)

        val restored = ProfileViewModel("jdoe", saved.afterProcessDeath(), repository)

        assertEquals(9, restored.state.value.selectedCursusId)
    }
}

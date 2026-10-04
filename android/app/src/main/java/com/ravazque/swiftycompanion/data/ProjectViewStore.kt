package com.ravazque.swiftycompanion.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.ravazque.swiftycompanion.model.ProfileGroup
import com.ravazque.swiftycompanion.model.ProjectSort
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.model.ProjectView

interface ProjectViewStore {
    fun load(group: ProfileGroup): ProjectView
    fun save(group: ProfileGroup, view: ProjectView)
}

class PrefsProjectViewStore(private val prefs: SharedPreferences) : ProjectViewStore {
    override fun load(group: ProfileGroup) = ProjectView(
        filter = ProjectStatus.entries.firstOrNull { it.name == prefs.getString(filterKey(group), null) },
        sort = ProjectSort.entries.firstOrNull { it.name == prefs.getString(sortKey(group), null) } ?: ProjectSort.DATE,
    )

    override fun save(group: ProfileGroup, view: ProjectView) {
        prefs.edit(commit = true) {
            putString(filterKey(group), view.filter?.name)
            putString(sortKey(group), view.sort.name)
        }
    }

    private fun filterKey(group: ProfileGroup) = "projects_filter_${group.name.lowercase()}"
    private fun sortKey(group: ProfileGroup) = "projects_sort_${group.name.lowercase()}"
}

package com.ravazque.swiftycompanion.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.ravazque.swiftycompanion.model.Visibility

interface VisibilityStore {
    fun load(): Visibility
    fun save(visibility: Visibility)
}

class PrefsVisibilityStore(private val prefs: SharedPreferences) : VisibilityStore {
    override fun load() = Visibility(
        staff = prefs.getBoolean(KEY_STAFF, false),
        blackholed = prefs.getBoolean(KEY_BLACKHOLED, false),
        frozen = prefs.getBoolean(KEY_FROZEN, false),
    )

    // Written at once: a toggle followed by closing the app must not be lost.
    override fun save(visibility: Visibility) {
        prefs.edit(commit = true) {
            putBoolean(KEY_STAFF, visibility.staff)
            putBoolean(KEY_BLACKHOLED, visibility.blackholed)
            putBoolean(KEY_FROZEN, visibility.frozen)
        }
    }

    private companion object {
        const val KEY_STAFF = "show_staff"
        const val KEY_BLACKHOLED = "show_blackholed"
        const val KEY_FROZEN = "show_frozen"
    }
}

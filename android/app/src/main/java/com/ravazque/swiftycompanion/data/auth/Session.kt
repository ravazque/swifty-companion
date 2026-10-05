package com.ravazque.swiftycompanion.data.auth

import android.content.SharedPreferences
import androidx.core.content.edit

// Who signed in and which version of the terms they accepted, and when. Nothing else is kept.
data class Session(val login: String, val termsVersion: Int, val acceptedAt: Long)

// A sign-in in progress: saved before the browser opens, since Android may kill the app meanwhile.
data class PendingLogin(val state: String, val verifier: String, val termsVersion: Int, val acceptedAt: Long)

interface SessionStore {
    fun loadSession(): Session?
    fun saveSession(session: Session?)
    fun loadPending(): PendingLogin?
    fun savePending(pending: PendingLogin?)
}

class PrefsSessionStore(private val prefs: SharedPreferences) : SessionStore {
    override fun loadSession(): Session? {
        val login = prefs.getString(KEY_LOGIN, null) ?: return null
        return Session(login, prefs.getInt(KEY_TERMS, 0), prefs.getLong(KEY_ACCEPTED_AT, 0L))
    }

    override fun saveSession(session: Session?) = prefs.edit(commit = true) {
        if (session == null) {
            remove(KEY_LOGIN).remove(KEY_TERMS).remove(KEY_ACCEPTED_AT)
        } else {
            putString(KEY_LOGIN, session.login)
            putInt(KEY_TERMS, session.termsVersion)
            putLong(KEY_ACCEPTED_AT, session.acceptedAt)
        }
    }

    override fun loadPending(): PendingLogin? {
        val state = prefs.getString(KEY_PENDING_STATE, null) ?: return null
        val verifier = prefs.getString(KEY_PENDING_VERIFIER, null) ?: return null
        return PendingLogin(state, verifier, prefs.getInt(KEY_PENDING_TERMS, 0), prefs.getLong(KEY_PENDING_ACCEPTED_AT, 0L))
    }

    override fun savePending(pending: PendingLogin?) = prefs.edit(commit = true) {
        if (pending == null) {
            remove(KEY_PENDING_STATE).remove(KEY_PENDING_VERIFIER).remove(KEY_PENDING_TERMS).remove(KEY_PENDING_ACCEPTED_AT)
        } else {
            putString(KEY_PENDING_STATE, pending.state)
            putString(KEY_PENDING_VERIFIER, pending.verifier)
            putInt(KEY_PENDING_TERMS, pending.termsVersion)
            putLong(KEY_PENDING_ACCEPTED_AT, pending.acceptedAt)
        }
    }

    private companion object {
        const val KEY_LOGIN = "session_login"
        const val KEY_TERMS = "session_terms_version"
        const val KEY_ACCEPTED_AT = "session_terms_accepted_at"
        const val KEY_PENDING_STATE = "pending_state"
        const val KEY_PENDING_VERIFIER = "pending_verifier"
        const val KEY_PENDING_TERMS = "pending_terms_version"
        const val KEY_PENDING_ACCEPTED_AT = "pending_terms_accepted_at"
    }
}

package com.ravazque.swiftycompanion.data.auth

import android.content.SharedPreferences
import androidx.core.content.edit

data class Token(val value: String, val expiresAt: Long)

interface TokenStore {
    fun load(): Token?
    fun save(token: Token)
}

class PrefsTokenStore(private val prefs: SharedPreferences) : TokenStore {
    override fun load(): Token? {
        val value = prefs.getString(KEY_VALUE, null) ?: return null
        return Token(value, prefs.getLong(KEY_EXPIRES_AT, 0L))
    }

    override fun save(token: Token) {
        prefs.edit {
            putString(KEY_VALUE, token.value)
            putLong(KEY_EXPIRES_AT, token.expiresAt)
        }
    }

    private companion object {
        const val KEY_VALUE = "access_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}

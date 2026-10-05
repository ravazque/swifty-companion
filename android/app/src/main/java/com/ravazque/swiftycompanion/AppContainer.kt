package com.ravazque.swiftycompanion

import android.content.Context
import com.ravazque.swiftycompanion.data.PrefsProjectViewStore
import com.ravazque.swiftycompanion.data.PrefsVisibilityStore
import com.ravazque.swiftycompanion.data.UserRepository
import com.ravazque.swiftycompanion.data.auth.PrefsSessionStore
import com.ravazque.swiftycompanion.data.auth.PrefsTokenStore
import com.ravazque.swiftycompanion.data.auth.SessionManager
import com.ravazque.swiftycompanion.data.net.ApiClient

// Manual dependency injection: one instance of each shared object for the whole process.
class AppContainer(context: Context) {
    private val auth = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val client = ApiClient(
        baseUrl = ApiClient.BASE_URL,
        clientId = BuildConfig.INTRA_CLIENT_ID,
        clientSecret = BuildConfig.INTRA_CLIENT_SECRET,
        store = PrefsTokenStore(auth),
    )

    private val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val tokens = client.tokens
    val repository = UserRepository(api = client.api, tokens = tokens, visibilityStore = PrefsVisibilityStore(settings))
    val projectViews = PrefsProjectViewStore(settings)
    val session = SessionManager(
        auth = client.auth,
        me = client.me,
        store = PrefsSessionStore(auth),
        clientId = BuildConfig.INTRA_CLIENT_ID,
        clientSecret = BuildConfig.INTRA_CLIENT_SECRET,
        authorizeUrl = ApiClient.BASE_URL + "oauth/authorize",
    )
}

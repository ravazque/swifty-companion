package com.ravazque.swiftycompanion

import android.content.Context
import com.ravazque.swiftycompanion.data.UserRepository
import com.ravazque.swiftycompanion.data.auth.PrefsTokenStore
import com.ravazque.swiftycompanion.data.net.ApiClient

// Manual dependency injection: one instance of each shared object for the whole process.
class AppContainer(context: Context) {
    private val client = ApiClient(
        baseUrl = ApiClient.BASE_URL,
        clientId = BuildConfig.INTRA_CLIENT_ID,
        clientSecret = BuildConfig.INTRA_CLIENT_SECRET,
        store = PrefsTokenStore(context.getSharedPreferences("auth", Context.MODE_PRIVATE)),
    )

    val repository = UserRepository(client.api, client.tokens)
}

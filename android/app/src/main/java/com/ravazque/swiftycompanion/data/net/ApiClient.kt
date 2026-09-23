package com.ravazque.swiftycompanion.data.net

import com.ravazque.swiftycompanion.data.auth.AuthApi
import com.ravazque.swiftycompanion.data.auth.AuthInterceptor
import com.ravazque.swiftycompanion.data.auth.TokenAuthenticator
import com.ravazque.swiftycompanion.data.auth.TokenManager
import com.ravazque.swiftycompanion.data.auth.TokenStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// Builds the HTTP stack. The token endpoint uses a client without the auth interceptor,
// otherwise asking for a token would itself need a token.
class ApiClient(
    baseUrl: String,
    clientId: String,
    clientSecret: String,
    store: TokenStore,
    clock: () -> Long = System::currentTimeMillis,
    sleep: (Long) -> Unit = Thread::sleep,
) {
    private val baseClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .addInterceptor(RateLimitInterceptor(sleep = sleep))
        .build()

    val tokens = TokenManager(
        api = retrofit(baseUrl, baseClient).create(AuthApi::class.java),
        store = store,
        clientId = clientId,
        clientSecret = clientSecret,
        clock = clock,
    )

    val api: IntraApi = retrofit(
        baseUrl,
        baseClient.newBuilder()
            .addInterceptor(AuthInterceptor(tokens))
            .authenticator(TokenAuthenticator(tokens))
            .build(),
    ).create(IntraApi::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(IntraJson.asConverterFactory("application/json".toMediaType()))
        .build()

    companion object {
        const val BASE_URL = "https://api.intra.42.fr/"
        private const val USER_AGENT = "SwiftyCompanion/1.0 (Android)"
    }
}

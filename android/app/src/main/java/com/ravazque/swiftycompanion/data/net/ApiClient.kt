package com.ravazque.swiftycompanion.data.net

import com.ravazque.swiftycompanion.data.auth.AuthApi
import com.ravazque.swiftycompanion.data.auth.AuthInterceptor
import com.ravazque.swiftycompanion.data.auth.CdnAuthInterceptor
import com.ravazque.swiftycompanion.data.auth.MeApi
import com.ravazque.swiftycompanion.data.auth.TokenAuthenticator
import com.ravazque.swiftycompanion.data.auth.TokenManager
import com.ravazque.swiftycompanion.data.auth.TokenStore
import okhttp3.Interceptor
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
    private val userAgent = Interceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
    }

    private val baseClient = OkHttpClient.Builder()
        .addInterceptor(userAgent)
        .addInterceptor(RateLimitInterceptor(sleep = sleep))
        .build()

    // Without the auth interceptor: the token endpoint and /v2/me with the signed-in user's token.
    val auth: AuthApi = retrofit(baseUrl, baseClient).create(AuthApi::class.java)
    val me: MeApi = retrofit(baseUrl, baseClient).create(MeApi::class.java)

    val tokens = TokenManager(
        api = auth,
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

    // For the image loader: CDN requests, outside the API rate limit, with the token only on profile photos.
    val images: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgent)
        .addInterceptor(CdnAuthInterceptor(tokens))
        .build()

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

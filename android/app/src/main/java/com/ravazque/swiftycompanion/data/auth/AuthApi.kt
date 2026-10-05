package com.ravazque.swiftycompanion.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class MeResponse(val login: String)

interface AuthApi {
    @FormUrlEncoded
    @POST("oauth/token")
    fun token(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
    ): Call<TokenResponse>

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun exchange(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String,
    ): TokenResponse
}

// Called with the signed-in user's token instead of the app's, so it goes through a client without the auth interceptor.
interface MeApi {
    @GET("v2/me")
    suspend fun me(@Header(AUTHORIZATION) authorization: String): MeResponse
}

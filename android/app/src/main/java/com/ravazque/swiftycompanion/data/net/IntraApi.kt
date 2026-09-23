package com.ravazque.swiftycompanion.data.net

import retrofit2.http.GET
import retrofit2.http.Path

interface IntraApi {
    @GET("v2/users/{login}")
    suspend fun user(@Path("login") login: String): UserDto

    @GET("v2/users/{login}/coalitions")
    suspend fun coalitions(@Path("login") login: String): List<CoalitionDto>
}

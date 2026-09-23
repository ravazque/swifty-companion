package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.data.auth.TokenException
import com.ravazque.swiftycompanion.data.auth.TokenManager
import com.ravazque.swiftycompanion.data.net.IntraApi
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class UserRepository(private val api: IntraApi, private val tokens: TokenManager) {
    private val cache = ConcurrentHashMap<String, Profile>()

    fun cached(login: String): Profile? = cache[login]

    suspend fun fetch(login: String): Profile {
        if (!tokens.hasCredentials) throw AppError.MissingCredentials
        try {
            val user = api.user(login)
            val coalition = try {
                api.coalitions(login).firstOrNull()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            return user.toProfile(coalition).also { cache[login] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toAppError(login)
        }
    }
}

private fun Exception.toAppError(login: String): AppError = when (this) {
    is AppError -> this
    is TokenException -> when (code) {
        400, 401 -> AppError.Unauthorized
        429 -> AppError.RateLimited
        else -> AppError.Server(code)
    }
    is HttpException -> when (val code = code()) {
        404 -> AppError.NotFound(login)
        401 -> AppError.Unauthorized
        403 -> AppError.Forbidden
        429 -> AppError.RateLimited
        else -> AppError.Server(code)
    }
    is SerializationException -> AppError.UnexpectedResponse
    is IOException -> AppError.Network
    else -> AppError.UnexpectedResponse
}

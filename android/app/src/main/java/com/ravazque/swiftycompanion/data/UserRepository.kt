package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.data.auth.TokenException
import com.ravazque.swiftycompanion.data.auth.TokenManager
import com.ravazque.swiftycompanion.data.net.IntraApi
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.SHOW_BLACKHOLED
import com.ravazque.swiftycompanion.model.isHidden
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class UserRepository(
    private val api: IntraApi,
    private val tokens: TokenManager,
    private val showBlackholed: Boolean = SHOW_BLACKHOLED,
    private val now: () -> Instant = Instant::now,
) {
    private val cache = ConcurrentHashMap<String, Profile>()

    fun cached(login: String): Profile? = cache[login]

    suspend fun fetch(login: String): Profile {
        if (!tokens.hasCredentials) throw AppError.MissingCredentials
        try {
            val profile = api.user(login).toProfile(now())
            // Checked before the coalition calls: a hidden profile costs a single request.
            if (profile.kind.isHidden(showBlackholed)) throw AppError.Hidden(login, profile.kind)
            val coalition = optional { api.coalitions(login).firstOrNull() }
            val score = coalition?.let { c -> optional { api.coalitionsUsers(login).firstOrNull { it.coalitionId == c.id }?.score } }
            return profile.copy(coalition = coalition?.toCoalition(score)).also { cache[login] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toAppError(login)
        }
    }
}

// The coalition is extra information: without it the profile still shows.
private suspend fun <T> optional(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

private fun Exception.toAppError(login: String): AppError = when (this) {
    is AppError -> this
    is TokenException -> when (code) {
        null -> AppError.UnexpectedResponse
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

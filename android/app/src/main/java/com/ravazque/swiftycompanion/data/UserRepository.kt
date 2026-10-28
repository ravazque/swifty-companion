package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.BuildConfig
import com.ravazque.swiftycompanion.data.auth.TokenException
import com.ravazque.swiftycompanion.data.auth.TokenManager
import com.ravazque.swiftycompanion.data.net.IntraApi
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.HOME_CAMPUS_ID
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.Visibility
import com.ravazque.swiftycompanion.model.isHidden
import com.ravazque.swiftycompanion.model.loginList
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class UserRepository(
    private val api: IntraApi,
    private val tokens: TokenManager,
    private val visibilityStore: VisibilityStore,
    // Answered as nonexistent without asking the API: BLOCKED_LOGINS in .env, so no login is published with the code.
    private val blockedLogins: Set<String> = loginList(BuildConfig.BLOCKED_LOGINS),
    private val now: () -> Instant = Instant::now,
) {
    private val cache = ConcurrentHashMap<String, Profile>()

    // Which hidden kinds the search shows anyway; kept across app restarts.
    var visibility: Visibility
        get() = visibilityStore.load()
        set(value) = visibilityStore.save(value)

    fun cached(login: String): Profile? = cache[login]

    suspend fun fetch(login: String): Profile {
        if (!tokens.hasCredentials) throw AppError.MissingCredentials
        if (login in blockedLogins) throw AppError.NotFound(login)
        try {
            val profile = api.user(login).toProfile(now())
            // Checked before the coalition calls: a profile that is not shown costs a single request.
            if (profile.campus?.id != HOME_CAMPUS_ID) throw AppError.OtherCampus(login, profile.campus?.name)
            if (profile.kind.isHidden(visibility)) throw AppError.Hidden(login, profile.kind)
            // The score is the user's own in that coalition (it can be negative).
            val coalition = optional { api.coalitions(login).mainCoalition() }?.let { main ->
                main.copy(score = optional { api.coalitionsUsers(login).firstOrNull { it.coalitionId == main.id }?.score })
            }
            return profile.copy(coalition = coalition).also { cache[login] = it }
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

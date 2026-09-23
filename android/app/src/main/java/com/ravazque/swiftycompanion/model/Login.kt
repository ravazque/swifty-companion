package com.ravazque.swiftycompanion.model

private val LOGIN_PATTERN = Regex("[a-z0-9_-]{1,32}")

// The login ends up in the request path, so anything outside the pattern is rejected before any call.
fun normalizeLogin(input: String): String {
    val login = input.trim().lowercase()
    if (login.isEmpty()) throw AppError.EmptyLogin
    if (!LOGIN_PATTERN.matches(login)) throw AppError.InvalidLogin
    return login
}

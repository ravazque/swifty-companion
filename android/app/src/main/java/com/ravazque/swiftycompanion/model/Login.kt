package com.ravazque.swiftycompanion.model

private val LOGIN_PATTERN = Regex("[a-z0-9_-]{1,32}")

// Accounts the app answers as nonexistent, without asking the API about them.
val BLOCKED_LOGINS = setOf(
    "madridqa", "madridqa2", "madridqa3", "madridtests", "tesrando", "tholzheu", "mrodrigu", "sraccah",
    "vir", "rookie", "bacon", "durum", "sanja", "sushi", "sandwichito", "el-fourbo", "senpai", "pmakarni",
    "irene", "racuenca", "leticia", "warrior", "svjix",
)

// The login ends up in the request path, so anything outside the pattern is rejected before any call.
fun normalizeLogin(input: String): String {
    val login = input.trim().lowercase()
    if (login.isEmpty()) throw AppError.EmptyLogin
    if (!LOGIN_PATTERN.matches(login)) throw AppError.InvalidLogin
    return login
}

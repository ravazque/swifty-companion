package com.ravazque.swiftycompanion.model

sealed class AppError : Exception() {
    data object EmptyLogin : AppError()
    data object InvalidLogin : AppError()
    data class NotFound(val login: String) : AppError()
    data object Network : AppError()
    data object RateLimited : AppError()
    data object MissingCredentials : AppError()
    data object Unauthorized : AppError()
    data object Forbidden : AppError()
    data class Server(val code: Int) : AppError()
    data object UnexpectedResponse : AppError()
}

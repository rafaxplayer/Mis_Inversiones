package com.misinversiones.app.data.auth

sealed class AuthState {
    object Initializing : AuthState()
    object Unauthenticated : AuthState()
    data class Unauthorized(val email: String, val reason: String) : AuthState()
    data class Authenticated(val email: String, val displayName: String?, val photoUrl: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

package com.m4.red_android.auth

sealed interface SessionState {
    data object Restoring : SessionState

    data object Unauthenticated : SessionState

    data class Authenticated(
        val session: AuthenticatedSession,
    ) : SessionState

    data class Expired(
        val reason: ExpirationReason,
    ) : SessionState
}

data class AuthenticatedSession(
    val accessToken: String,
    val expiresAtEpochSeconds: Long,
    val user: SessionUser? = null,
)

data class SessionUser(
    val name: String,
    val role: String,
    val companyId: String,
    val requiresInitialPasswordChange: Boolean = false,
)

enum class ExpirationReason {
    LOCAL_EXPIRY,
    UNAUTHORIZED,
}

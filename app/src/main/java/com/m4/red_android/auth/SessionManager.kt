package com.m4.red_android.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionManager(
    private val tokenStore: SecureTokenStore,
    private val epochSeconds: () -> Long = { System.currentTimeMillis() / 1_000 },
) {
    private val transitionMutex = Mutex()
    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)

    val state: StateFlow<SessionState> = _state.asStateFlow()

    suspend fun restore() = transitionMutex.withLock {
        val restored = try {
            tokenStore.read()
        } catch (_: Exception) {
            runCatching { tokenStore.clear() }
            _state.value = SessionState.Unauthenticated
            return@withLock
        }
        when {
            restored == null -> _state.value = SessionState.Unauthenticated
            restored.isExpired() -> {
                tokenStore.clear()
                _state.value = SessionState.Expired(ExpirationReason.LOCAL_EXPIRY)
            }
            else -> _state.value = SessionState.Authenticated(restored)
        }
    }

    suspend fun authenticate(session: AuthenticatedSession) = transitionMutex.withLock {
        require(session.accessToken.isNotBlank()) { "Access token must not be blank" }
        require(!session.isExpired()) { "Cannot authenticate an expired session" }

        tokenStore.write(session)
        _state.value = SessionState.Authenticated(session)
    }

    suspend fun replaceToken(
        expectedAccessToken: String,
        replacement: AuthenticatedSession,
    ): Boolean = transitionMutex.withLock {
        val current = (_state.value as? SessionState.Authenticated)?.session
        if (current?.accessToken != expectedAccessToken || replacement.isExpired() ||
            replacement.expiresAtEpochSeconds < current.expiresAtEpochSeconds
        ) {
            return@withLock false
        }

        tokenStore.write(replacement)
        _state.value = SessionState.Authenticated(replacement)
        true
    }

    suspend fun markPasswordChanged() = transitionMutex.withLock {
        val current = (_state.value as? SessionState.Authenticated)?.session ?: return@withLock
        val user = current.user ?: return@withLock
        val replacement = current.copy(user = user.copy(requiresInitialPasswordChange = false))
        tokenStore.write(replacement)
        _state.value = SessionState.Authenticated(replacement)
    }

    suspend fun expireIfActive(accessToken: String): Boolean = transitionMutex.withLock {
        val current = (_state.value as? SessionState.Authenticated)?.session
        if (current?.accessToken != accessToken) {
            return@withLock false
        }

        tokenStore.clear()
        _state.value = SessionState.Expired(ExpirationReason.UNAUTHORIZED)
        true
    }

    suspend fun expireIfNecessary(): Boolean = transitionMutex.withLock {
        val current = (_state.value as? SessionState.Authenticated)?.session
        if (current == null || !current.isExpired()) {
            return@withLock false
        }

        tokenStore.clear()
        _state.value = SessionState.Expired(ExpirationReason.LOCAL_EXPIRY)
        true
    }

    suspend fun logout() = transitionMutex.withLock {
        try {
            tokenStore.clear()
        } finally {
            _state.value = SessionState.Unauthenticated
        }
    }

    private fun AuthenticatedSession.isExpired(): Boolean =
        expiresAtEpochSeconds <= epochSeconds()
}

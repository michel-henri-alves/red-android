package com.m4.red_android.auth

/** Persistence boundary implemented by [KeystoreSecureTokenStore] in production. */
interface SecureTokenStore {
    suspend fun read(): AuthenticatedSession?

    suspend fun write(session: AuthenticatedSession)

    suspend fun clear()
}

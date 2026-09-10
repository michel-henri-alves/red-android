package com.m4.red_android.auth

class FakeSecureTokenStore(
    var storedSession: AuthenticatedSession? = null,
) : SecureTokenStore {
    var readFailure: Throwable? = null
    var writeFailure: Throwable? = null
    var clearFailure: Throwable? = null
    var writeCount = 0
    var clearCount = 0

    override suspend fun read(): AuthenticatedSession? {
        readFailure?.let { throw it }
        return storedSession
    }

    override suspend fun write(session: AuthenticatedSession) {
        writeCount++
        writeFailure?.let { throw it }
        storedSession = session
    }

    override suspend fun clear() {
        clearCount++
        clearFailure?.let { throw it }
        storedSession = null
    }
}

package com.m4.red_android.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SessionManagerTest {
    private val now = 1_000L

    @Test
    fun startsRestoringAndRestoresAValidPersistedSession() = runTest {
        val persisted = session("persisted", expiresAt = now + 60)
        val manager = SessionManager(FakeSecureTokenStore(persisted)) { now }

        assertSame(SessionState.Restoring, manager.state.value)

        manager.restore()

        assertEquals(SessionState.Authenticated(persisted), manager.state.value)
    }

    @Test
    fun restoredRecoverySessionKeepsMandatoryPasswordChangeAfterProcessRecreation() = runTest {
        val persisted = AuthenticatedSession(
            accessToken = "restricted-token",
            expiresAtEpochSeconds = now + 60,
            user = SessionUser(
                name = "Recovery User",
                role = "user",
                companyId = "company-1",
                requiresInitialPasswordChange = true,
            ),
        )
        val store = FakeSecureTokenStore(persisted)

        val recreatedManager = SessionManager(store) { now }
        recreatedManager.restore()

        assertEquals(SessionState.Authenticated(persisted), recreatedManager.state.value)
    }

    @Test
    fun missingPersistedSessionCompletesRestoreAsUnauthenticated() = runTest {
        val manager = SessionManager(FakeSecureTokenStore()) { now }

        manager.restore()

        assertSame(SessionState.Unauthenticated, manager.state.value)
    }

    @Test
    fun unreadablePersistedSessionIsClearedAndFailsClosed() = runTest {
        val store = FakeSecureTokenStore().apply {
            readFailure = IllegalStateException("corrupt encrypted record")
        }
        val manager = SessionManager(store) { now }

        manager.restore()

        assertEquals(1, store.clearCount)
        assertSame(SessionState.Unauthenticated, manager.state.value)
    }

    @Test
    fun expiredPersistedSessionIsClearedAndExposedAsExpired() = runTest {
        val store = FakeSecureTokenStore(session("expired", expiresAt = now))
        val manager = SessionManager(store) { now }

        manager.restore()

        assertNull(store.storedSession)
        assertEquals(1, store.clearCount)
        assertEquals(
            SessionState.Expired(ExpirationReason.LOCAL_EXPIRY),
            manager.state.value,
        )
    }

    @Test
    fun authenticatePublishesSessionOnlyAfterPersistenceSucceeds() = runTest {
        val failure = IllegalStateException("disk unavailable")
        val store = FakeSecureTokenStore().apply { writeFailure = failure }
        val manager = SessionManager(store) { now }
        manager.restore()

        try {
            manager.authenticate(session("new", expiresAt = now + 60))
            fail("Expected persistence failure")
        } catch (caught: IllegalStateException) {
            assertSame(failure, caught)
        }

        assertSame(SessionState.Unauthenticated, manager.state.value)
        assertNull(store.storedSession)
    }

    @Test
    fun newerReplacementIsPersistedButStaleOrOlderReplacementIsIgnored() = runTest {
        val initial = session("token-1", expiresAt = now + 60)
        val store = FakeSecureTokenStore(initial)
        val manager = SessionManager(store) { now }
        manager.restore()

        assertTrue(
            manager.replaceToken(
                expectedAccessToken = "token-1",
                replacement = session("token-2", expiresAt = now + 120),
            ),
        )
        assertFalse(
            manager.replaceToken(
                expectedAccessToken = "token-1",
                replacement = session("stale", expiresAt = now + 180),
            ),
        )
        assertFalse(
            manager.replaceToken(
                expectedAccessToken = "token-2",
                replacement = session("older", expiresAt = now + 30),
            ),
        )

        assertEquals("token-2", store.storedSession?.accessToken)
        assertEquals(1, store.writeCount)
    }

    @Test
    fun unauthorizedResponseExpiresOnlyTheSessionThatMadeTheRequest() = runTest {
        val store = FakeSecureTokenStore(session("old", expiresAt = now + 60))
        val manager = SessionManager(store) { now }
        manager.restore()
        manager.replaceToken("old", session("current", expiresAt = now + 120))

        assertFalse(manager.expireIfActive("old"))
        assertTrue(manager.state.value is SessionState.Authenticated)
        assertTrue(manager.expireIfActive("current"))

        assertNull(store.storedSession)
        assertEquals(
            SessionState.Expired(ExpirationReason.UNAUTHORIZED),
            manager.state.value,
        )
    }

    @Test
    fun localExpiryAndLogoutClearPersistenceAndPublishActionableStates() = runTest {
        var clock = now
        val store = FakeSecureTokenStore(session("active", expiresAt = now + 10))
        val manager = SessionManager(store) { clock }
        manager.restore()

        assertFalse(manager.expireIfNecessary())
        clock += 10
        assertTrue(manager.expireIfNecessary())
        assertEquals(SessionState.Expired(ExpirationReason.LOCAL_EXPIRY), manager.state.value)

        manager.authenticate(session("again", expiresAt = clock + 60))
        manager.logout()

        assertNull(store.storedSession)
        assertSame(SessionState.Unauthenticated, manager.state.value)
    }

    private fun session(token: String, expiresAt: Long) = AuthenticatedSession(
        accessToken = token,
        expiresAtEpochSeconds = expiresAt,
        user = SessionUser("User", "seller", "company-1"),
    )
}

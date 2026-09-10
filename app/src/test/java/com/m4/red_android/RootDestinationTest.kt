package com.m4.red_android

import com.m4.red_android.auth.AuthenticatedSession
import com.m4.red_android.auth.ExpirationReason
import com.m4.red_android.auth.SessionState
import com.m4.red_android.auth.SessionUser
import org.junit.Assert.assertEquals
import org.junit.Test

class RootDestinationTest {
    @Test fun `restoring never selects login or protected content early`() {
        assertEquals(RootDestination.RESTORING, SessionState.Restoring.rootDestination())
    }

    @Test fun `unauthenticated selects login`() {
        assertEquals(RootDestination.LOGIN, SessionState.Unauthenticated.rootDestination())
    }

    @Test fun `expired session selects login with expiry context`() {
        assertEquals(
            RootDestination.EXPIRED_LOGIN,
            SessionState.Expired(ExpirationReason.UNAUTHORIZED).rootDestination(),
        )
    }

    @Test fun `authenticated session alone selects protected content`() {
        assertEquals(
            RootDestination.PROTECTED,
            SessionState.Authenticated(AuthenticatedSession("token", Long.MAX_VALUE)).rootDestination(),
        )
    }

    @Test fun `temporary password session selects mandatory password change`() {
        val user = SessionUser("Maria", "USER", "company-1", requiresInitialPasswordChange = true)
        assertEquals(
            RootDestination.PASSWORD_CHANGE,
            SessionState.Authenticated(AuthenticatedSession("token", Long.MAX_VALUE, user)).rootDestination(),
        )
    }
}

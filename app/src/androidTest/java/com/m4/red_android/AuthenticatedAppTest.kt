package com.m4.red_android

import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.m4.red_android.auth.AuthenticatedSession
import com.m4.red_android.auth.SecureTokenStore
import com.m4.red_android.auth.SessionManager
import com.m4.red_android.auth.SessionUser
import com.m4.red_android.data.api.ChangePasswordRequest
import com.m4.red_android.data.api.ChangePasswordResponse
import com.m4.red_android.data.api.LoginApi
import com.m4.red_android.data.api.LoginRequest
import com.m4.red_android.data.api.LoginResponse
import com.m4.red_android.data.api.PasswordApi
import com.m4.red_android.data.api.PasswordRecoveryRequest
import com.m4.red_android.data.api.PasswordRecoveryResponse
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class AuthenticatedAppTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun restrictedRestoredSessionNeverComposesProtectedNavigationOnBack() {
        val manager = SessionManager(InMemoryTokenStore(), epochSeconds = { 1_000 })
        runBlocking {
            manager.authenticate(
                AuthenticatedSession(
                    accessToken = "restricted-token",
                    expiresAtEpochSeconds = 2_000,
                    user = SessionUser("Recovery User", "user", "company-1", true),
                ),
            )
        }

        composeRule.setContent {
            BackHandler { /* Root back is consumed while mandatory change is active. */ }
            AuthenticatedApp(manager, NoOpLoginApi, NoOpPasswordApi)
        }

        composeRule.onNodeWithText("Crie sua senha pessoal").assertIsDisplayed()
        composeRule.onAllNodesWithText("Vendas").assertCountEquals(0)
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Crie sua senha pessoal").assertIsDisplayed()
        composeRule.onAllNodesWithText("Vendas").assertCountEquals(0)
    }
}

private class InMemoryTokenStore : SecureTokenStore {
    private var session: AuthenticatedSession? = null
    override suspend fun read(): AuthenticatedSession? = session
    override suspend fun write(session: AuthenticatedSession) { this.session = session }
    override suspend fun clear() { session = null }
}

private object NoOpLoginApi : LoginApi {
    override suspend fun login(request: LoginRequest): LoginResponse = error("Not expected")
    override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest): PasswordRecoveryResponse =
        error("Not expected")
}

private object NoOpPasswordApi : PasswordApi {
    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse =
        error("Not expected")
}

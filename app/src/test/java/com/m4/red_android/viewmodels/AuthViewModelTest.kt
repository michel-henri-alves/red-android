package com.m4.red_android.viewmodels

import com.m4.red_android.auth.FakeSecureTokenStore
import com.m4.red_android.auth.SessionManager
import com.m4.red_android.auth.SessionState
import com.m4.red_android.data.api.LoginApi
import com.m4.red_android.data.api.LoginRequest
import com.m4.red_android.data.api.LoginResponse
import com.m4.red_android.data.api.LoginUser
import com.m4.red_android.data.api.PasswordRecoveryRequest
import com.m4.red_android.data.api.PasswordRecoveryResponse
import com.m4.red_android.data.api.ChangePasswordRequest
import com.m4.red_android.data.api.ChangePasswordResponse
import com.m4.red_android.data.api.PasswordApi
import com.m4.red_android.auth.AuthenticatedSession
import com.m4.red_android.auth.SessionUser
import com.m4.red_android.testing.MainDispatcherRule
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `successful login persists authenticated session and user`() = runTest(mainDispatcherRule.dispatcher) {
        val store = FakeSecureTokenStore()
        val manager = SessionManager(store, epochSeconds = { 1_000 })
        val viewModel = AuthViewModel(manager, apiReturning(successResponse()))

        viewModel.login(" company-1 ", " maria@example.com ", "secret")
        advanceUntilIdle()

        val session = (manager.state.value as SessionState.Authenticated).session
        assertEquals("Maria", session.user?.name)
        assertEquals("company-1", session.user?.companyId)
        assertEquals(session, store.storedSession)
        assertEquals(LoginUiState.Idle, viewModel.loginState.value)
    }

    @Test fun `invalid credentials remain signed out with actionable error`() = runTest(mainDispatcherRule.dispatcher) {
        val manager = restoredManager()
        val unauthorized = HttpException(
            Response.error<LoginResponse>(
                401,
                "{}".toResponseBody("application/json".toMediaType()),
            ),
        )
        val viewModel = AuthViewModel(manager, apiThrowing(unauthorized))

        viewModel.login("company-1", "maria@example.com", "wrong")
        advanceUntilIdle()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertEquals(LoginUiState.Error(LoginFailure.INVALID_CREDENTIALS), viewModel.loginState.value)
    }

    @Test fun `connectivity failure preserves local signed-out state`() = runTest(mainDispatcherRule.dispatcher) {
        val manager = restoredManager()
        val viewModel = AuthViewModel(manager, apiThrowing(IOException("offline")))

        viewModel.login("company-1", "maria@example.com", "secret")
        advanceUntilIdle()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertEquals(LoginUiState.Error(LoginFailure.CONNECTIVITY), viewModel.loginState.value)
    }

    @Test fun `malformed token is rejected without exposing protected state`() = runTest(mainDispatcherRule.dispatcher) {
        val manager = restoredManager()
        val response = successResponse().copy(accessToken = "not-a-jwt")
        val viewModel = AuthViewModel(manager, apiReturning(response))

        viewModel.login("company-1", "maria@example.com", "secret")
        advanceUntilIdle()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertEquals(LoginUiState.Error(LoginFailure.INVALID_RESPONSE), viewModel.loginState.value)
    }

    @Test fun `logout clears persisted session and protected state`() = runTest(mainDispatcherRule.dispatcher) {
        val store = FakeSecureTokenStore()
        val manager = SessionManager(store, epochSeconds = { 1_000 })
        manager.authenticate(
            com.m4.red_android.auth.AuthenticatedSession(jwt(2_000), 2_000),
        )
        val viewModel = AuthViewModel(manager, apiReturning(successResponse()))

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertEquals(1, store.clearCount)
        assertTrue(store.storedSession == null)
    }

    @Test fun `recovery sends normalized tenant identity and reaches generic accepted state`() = runTest(mainDispatcherRule.dispatcher) {
        var captured: PasswordRecoveryRequest? = null
        val api = object : LoginApi {
            override suspend fun login(request: LoginRequest) = successResponse()
            override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest): PasswordRecoveryResponse {
                captured = request
                return PasswordRecoveryResponse("password.recovery.request.accepted")
            }
        }
        val viewModel = AuthViewModel(restoredManager(), api)

        viewModel.requestPasswordRecovery(" company-1 ", " maria@example.com ")
        advanceUntilIdle()

        assertEquals(PasswordRecoveryRequest("company-1", "maria@example.com"), captured)
        assertEquals(RecoveryUiState.Accepted, viewModel.recoveryState.value)
    }

    @Test fun `recovery maps throttle response without exposing account state`() = runTest(mainDispatcherRule.dispatcher) {
        val throttled = HttpException(
            Response.error<PasswordRecoveryResponse>(
                429,
                "{}".toResponseBody("application/json".toMediaType()),
            ),
        )
        val viewModel = AuthViewModel(restoredManager(), apiThrowing(throttled))

        viewModel.requestPasswordRecovery("company-1", "maria@example.com")
        advanceUntilIdle()

        assertEquals(RecoveryUiState.Error(RecoveryFailure.THROTTLED), viewModel.recoveryState.value)
    }

    @Test fun `recovery maps connectivity failure to retryable state`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AuthViewModel(restoredManager(), apiThrowing(IOException("offline")))

        viewModel.requestPasswordRecovery("company-1", "maria@example.com")
        advanceUntilIdle()

        assertEquals(RecoveryUiState.Error(RecoveryFailure.CONNECTIVITY), viewModel.recoveryState.value)
    }

    @Test fun `recovery suppresses duplicate submission while request is loading`() = runTest(mainDispatcherRule.dispatcher) {
        var calls = 0
        val api = object : LoginApi {
            override suspend fun login(request: LoginRequest) = successResponse()
            override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest): PasswordRecoveryResponse {
                calls += 1
                return PasswordRecoveryResponse("password.recovery.request.accepted")
            }
        }
        val viewModel = AuthViewModel(restoredManager(), api)

        viewModel.requestPasswordRecovery("company-1", "maria@example.com")
        viewModel.requestPasswordRecovery("company-1", "maria@example.com")
        advanceUntilIdle()

        assertEquals(1, calls)
        assertEquals(RecoveryUiState.Accepted, viewModel.recoveryState.value)
    }

    @Test fun `successful mandatory password change unlocks the stored session`() = runTest(mainDispatcherRule.dispatcher) {
        val store = FakeSecureTokenStore()
        val manager = SessionManager(store, epochSeconds = { 1_000 })
        manager.authenticate(
            AuthenticatedSession(
                jwt(2_000),
                2_000,
                SessionUser("Maria", "USER", "company-1", requiresInitialPasswordChange = true),
            ),
        )
        var captured: ChangePasswordRequest? = null
        val passwordApi = object : PasswordApi {
            override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
                captured = request
                return ChangePasswordResponse("password.changed.successfully")
            }
        }
        val viewModel = AuthViewModel(manager, apiReturning(successResponse()), passwordApi)

        viewModel.changePassword("Temporary1!", "Stronger123!", "Stronger123!")
        advanceUntilIdle()

        assertEquals(ChangePasswordRequest("Temporary1!", "Stronger123!", "Stronger123!"), captured)
        val session = (manager.state.value as SessionState.Authenticated).session
        assertEquals(false, session.user?.requiresInitialPasswordChange)
        assertEquals(PasswordChangeState.Idle, viewModel.passwordChangeState.value)
    }

    private suspend fun restoredManager(): SessionManager =
        SessionManager(FakeSecureTokenStore(), epochSeconds = { 1_000 }).also { it.restore() }

    private fun successResponse() = LoginResponse(
        accessToken = jwt(2_000),
        user = LoginUser("Maria", "ADMIN", "company-1"),
    )

    private fun apiReturning(response: LoginResponse) = object : LoginApi {
        override suspend fun login(request: LoginRequest): LoginResponse = response
        override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest) =
            PasswordRecoveryResponse("password.recovery.request.accepted")
    }

    private fun apiThrowing(error: Exception) = object : LoginApi {
        override suspend fun login(request: LoginRequest): LoginResponse = throw error
        override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest): PasswordRecoveryResponse = throw error
    }

    private fun jwt(expiry: Long): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf("{}", "{\"exp\":$expiry}", "signature")
            .joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }
}

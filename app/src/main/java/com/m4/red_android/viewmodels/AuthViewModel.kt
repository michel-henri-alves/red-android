package com.m4.red_android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.m4.red_android.auth.AuthenticatedSession
import com.m4.red_android.auth.SessionManager
import com.m4.red_android.auth.SessionUser
import com.m4.red_android.data.api.JwtExpiryDecoder
import com.m4.red_android.data.api.LoginApi
import com.m4.red_android.data.api.LoginRequest
import com.m4.red_android.data.api.PasswordRecoveryRequest
import com.m4.red_android.data.api.ChangePasswordRequest
import com.m4.red_android.data.api.PasswordApi
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AuthViewModel(
    private val sessionManager: SessionManager,
    private val loginApi: LoginApi,
    private val passwordApi: PasswordApi? = null,
) : ViewModel() {
    val sessionState = sessionManager.state

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()
    private val _recoveryState = MutableStateFlow<RecoveryUiState>(RecoveryUiState.Idle)
    val recoveryState: StateFlow<RecoveryUiState> = _recoveryState.asStateFlow()
    private val _passwordChangeState = MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState.asStateFlow()

    fun login(companyId: String, email: String, password: String) {
        if (_loginState.value == LoginUiState.Loading) return
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            _loginState.value = try {
                val response = loginApi.login(LoginRequest(companyId.trim(), email.trim(), password))
                val expiry = JwtExpiryDecoder.expiry(response.accessToken)
                    ?: return@launch run { _loginState.value = LoginUiState.Error(LoginFailure.INVALID_RESPONSE) }
                sessionManager.authenticate(
                    AuthenticatedSession(
                        accessToken = response.accessToken,
                        expiresAtEpochSeconds = expiry,
                        user = SessionUser(
                            name = response.user.name,
                            role = response.user.role,
                            companyId = response.user.companyId,
                            requiresInitialPasswordChange = response.user.requiresInitialPasswordChange,
                        ),
                    ),
                )
                LoginUiState.Idle
            } catch (error: HttpException) {
                LoginUiState.Error(
                    if (error.code() == 401) LoginFailure.INVALID_CREDENTIALS else LoginFailure.SERVER,
                )
            } catch (_: IOException) {
                LoginUiState.Error(LoginFailure.CONNECTIVITY)
            } catch (_: Exception) {
                LoginUiState.Error(LoginFailure.SERVER)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (_passwordChangeState.value == PasswordChangeState.Loading) return
        if (newPassword.length < 12 || newPassword != confirmPassword) {
            _passwordChangeState.value = PasswordChangeState.Error("Confira a nova senha e sua confirmação.")
            return
        }
        viewModelScope.launch {
            _passwordChangeState.value = PasswordChangeState.Loading
            _passwordChangeState.value = try {
                val api = passwordApi ?: error("Password API is unavailable")
                api.changePassword(ChangePasswordRequest(currentPassword, newPassword, confirmPassword))
                sessionManager.markPasswordChanged()
                PasswordChangeState.Idle
            } catch (error: HttpException) {
                PasswordChangeState.Error(if (error.code() == 401) "Senha temporária inválida." else "Não foi possível alterar a senha.")
            } catch (_: IOException) {
                PasswordChangeState.Error("Sem conexão. Tente novamente.")
            } catch (_: Exception) {
                PasswordChangeState.Error("Não foi possível alterar a senha.")
            }
        }
    }

    fun requestPasswordRecovery(companyId: String, email: String) {
        if (_recoveryState.value == RecoveryUiState.Loading) return
        if (companyId.isBlank() || email.isBlank()) {
            _recoveryState.value = RecoveryUiState.Error(RecoveryFailure.VALIDATION)
            return
        }
        _recoveryState.value = RecoveryUiState.Loading
        viewModelScope.launch {
            _recoveryState.value = try {
                loginApi.requestPasswordRecovery(PasswordRecoveryRequest(companyId.trim(), email.trim()))
                RecoveryUiState.Accepted
            } catch (error: HttpException) {
                if (error.code() == 429) RecoveryUiState.Error(RecoveryFailure.THROTTLED)
                else RecoveryUiState.Error(RecoveryFailure.SERVER)
            } catch (_: IOException) {
                RecoveryUiState.Error(RecoveryFailure.CONNECTIVITY)
            } catch (_: Exception) {
                RecoveryUiState.Error(RecoveryFailure.SERVER)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { sessionManager.logout() }
            _loginState.value = LoginUiState.Idle
        }
    }

    class Factory(
        private val sessionManager: SessionManager,
        private val loginApi: LoginApi,
        private val passwordApi: PasswordApi? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            return AuthViewModel(sessionManager, loginApi, passwordApi) as T
        }
    }
}

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val failure: LoginFailure) : LoginUiState
}

enum class LoginFailure {
    INVALID_CREDENTIALS,
    CONNECTIVITY,
    SERVER,
    INVALID_RESPONSE,
}

sealed interface RecoveryUiState {
    data object Idle : RecoveryUiState
    data object Loading : RecoveryUiState
    data object Accepted : RecoveryUiState
    data class Error(val failure: RecoveryFailure) : RecoveryUiState
}

enum class RecoveryFailure { VALIDATION, THROTTLED, CONNECTIVITY, SERVER }

sealed interface PasswordChangeState {
    data object Idle : PasswordChangeState
    data object Loading : PasswordChangeState
    data class Error(val message: String) : PasswordChangeState
}

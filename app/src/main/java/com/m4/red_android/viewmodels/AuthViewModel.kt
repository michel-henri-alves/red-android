package com.m4.red_android.viewmodels

import com.m4.red_android.auth.CompanyContext
import com.m4.red_android.auth.CompanyContextStore
import com.m4.red_android.auth.SessionState
import com.m4.red_android.auth.validAccessName
import com.m4.red_android.data.api.CompanyAccessApi
import com.m4.red_android.data.api.CompanyAccessRequest
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val companyApi: CompanyAccessApi? = null,
    private val companyStore: CompanyContextStore? = null,
) : ViewModel() {
    val sessionState = sessionManager.state

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()
    private val _recoveryState = MutableStateFlow<RecoveryUiState>(RecoveryUiState.Idle)
    val recoveryState: StateFlow<RecoveryUiState> = _recoveryState.asStateFlow()
    private val _passwordChangeState = MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState.asStateFlow()

    private val _companyState = MutableStateFlow(CompanyUiState(restoring = companyStore != null))
    val companyState: StateFlow<CompanyUiState> = _companyState.asStateFlow()
    private var savedCompany: CompanyContext? = null
    private var selectionVersion = 0
    private var resolveJob: Job? = null
    private val companyMutex = Mutex()

    init {
        if (companyStore != null) viewModelScope.launch {
            savedCompany = companyStore.read()
            _companyState.value = CompanyUiState(company = savedCompany)
        }
    }

    fun selectCompany(input: String) {
        if (sessionState.value is SessionState.Authenticated || _companyState.value.restoring) return
        val accessName = input.trim().lowercase(Locale.ROOT)
        resolveJob?.cancel()
        val version = ++selectionVersion
        if (!validAccessName(accessName)) {
            _companyState.value = CompanyUiState(error = "Informe o nome de acesso da empresa, como minha-loja.")
            return
        }
        _companyState.value = CompanyUiState(loading = true)
        resolveJob = viewModelScope.launch {
            try {
                val company = requireNotNull(companyApi).resolve(CompanyAccessRequest(accessName)).toContext()
                require(company.isValid() && company.accessName == accessName)
                if (version == selectionVersion) _companyState.value = CompanyUiState(company = company)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (version == selectionVersion) _companyState.value = CompanyUiState(error = companyError(error))
            }
        }
    }

    fun switchCompany() {
        // Commit and switching are serialized; an authenticated session always requires logout first.
        viewModelScope.launch {
            companyMutex.withLock {
                if (sessionState.value is SessionState.Authenticated || _companyState.value.restoring) return@withLock
                ++selectionVersion
                resolveJob?.cancel()
                _companyState.value = CompanyUiState()
                _loginState.value = LoginUiState.Idle
                _recoveryState.value = RecoveryUiState.Idle
            }
        }
    }

    fun restoreCompanySelection() {
        if (sessionState.value is SessionState.Authenticated) return
        ++selectionVersion
        resolveJob?.cancel()
        _companyState.value = CompanyUiState(company = savedCompany)
    }

    private suspend fun revalidateCompany(companyId: String, version: Int): CompanyContext? {
        val api = companyApi ?: return null // Compatibility for legacy hosts/tests; production always injects resolver/store.
        val selected = _companyState.value.company ?: error("Company not selected")
        require(selected.companyId == companyId)
        val fresh = try { api.resolve(CompanyAccessRequest(selected.accessName)).toContext() }
        catch (error: HttpException) {
            if (version == selectionVersion && error.code() == 404) _companyState.value = CompanyUiState(error = companyError(error))
            throw error
        }
        if (version != selectionVersion) return null
        if (!fresh.isValid() || fresh.accessName != selected.accessName || fresh.companyId != selected.companyId) {
            _companyState.value = CompanyUiState(error = "A identificação da empresa mudou. Selecione a empresa novamente.")
            error("Company mapping mismatch")
        }
        return fresh
    }

    fun login(companyId: String, email: String, password: String) {
        if (_loginState.value == LoginUiState.Loading) return
        val version = selectionVersion
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            val result = try {
                val confirmed = revalidateCompany(companyId.trim(), version)
                if (version != selectionVersion) return@launch
                val response = loginApi.login(LoginRequest(companyId.trim(), email.trim(), password))
                if (version != selectionVersion) return@launch
                val expiry = JwtExpiryDecoder.expiry(response.accessToken)
                    ?: return@launch run { _loginState.value = LoginUiState.Error(LoginFailure.INVALID_RESPONSE) }
                if (response.user.companyId != companyId.trim()) {
                    _loginState.value = LoginUiState.Error(LoginFailure.INVALID_RESPONSE)
                    return@launch
                }
                companyMutex.withLock {
                if (version != selectionVersion) return@withLock
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
                if (confirmed != null) {
                    try { requireNotNull(companyStore).write(confirmed) }
                    catch (error: Exception) { sessionManager.logout(); throw error }
                    savedCompany = confirmed
                    _companyState.value = CompanyUiState(company = confirmed)
                }
                }
                LoginUiState.Idle
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: HttpException) {
                LoginUiState.Error(
                    when (error.code()) { 401 -> LoginFailure.INVALID_CREDENTIALS; 429 -> LoginFailure.THROTTLED; else -> LoginFailure.SERVER },
                )
            } catch (_: IOException) {
                LoginUiState.Error(LoginFailure.CONNECTIVITY)
            } catch (_: Exception) {
                LoginUiState.Error(LoginFailure.SERVER)
            }
            if (version == selectionVersion) _loginState.value = result
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
        val version = selectionVersion
        _recoveryState.value = RecoveryUiState.Loading
        viewModelScope.launch {
            val result = try {
                revalidateCompany(companyId.trim(), version)
                if (version != selectionVersion) return@launch
                loginApi.requestPasswordRecovery(PasswordRecoveryRequest(companyId.trim(), email.trim()))
                RecoveryUiState.Accepted
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: HttpException) {
                if (error.code() == 429) RecoveryUiState.Error(RecoveryFailure.THROTTLED)
                else RecoveryUiState.Error(RecoveryFailure.SERVER)
            } catch (_: IOException) {
                RecoveryUiState.Error(RecoveryFailure.CONNECTIVITY)
            } catch (_: Exception) {
                RecoveryUiState.Error(RecoveryFailure.SERVER)
            }
            if (version == selectionVersion) _recoveryState.value = result
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
        private val companyApi: CompanyAccessApi? = null,
        private val companyStore: CompanyContextStore? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            return AuthViewModel(sessionManager, loginApi, passwordApi, companyApi, companyStore) as T
        }
    }
}

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val failure: LoginFailure) : LoginUiState
}

enum class LoginFailure {
    THROTTLED,
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

data class CompanyUiState(
    val company: CompanyContext? = null,
    val loading: Boolean = false,
    val restoring: Boolean = false,
    val error: String? = null,
)
private fun companyError(error: Exception): String = when {
    error is IOException -> "Sem conexão. Verifique sua internet e tente novamente."
    error is HttpException && error.code() == 429 -> "Muitas tentativas. Aguarde antes de tentar novamente."
    error is HttpException && error.code() == 404 -> "Empresa indisponível. Confira o nome de acesso."
    else -> "Não foi possível identificar a empresa. Tente novamente."
}

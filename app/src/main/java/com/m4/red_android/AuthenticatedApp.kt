package com.m4.red_android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.m4.red_android.auth.CompanyContextStore
import com.m4.red_android.data.api.CompanyAccessApi
import com.m4.red_android.ui.login.CompanyAccessScreen
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.m4.red_android.auth.SessionManager
import com.m4.red_android.auth.SessionState
import com.m4.red_android.data.api.LoginApi
import com.m4.red_android.data.api.PasswordApi
import com.m4.red_android.ui.login.LoginScreen
import com.m4.red_android.ui.login.ChangePasswordScreen
import com.m4.red_android.ui.permissions.CameraPermissionHandler
import com.m4.red_android.viewmodels.AuthViewModel

internal enum class RootDestination { RESTORING, LOGIN, EXPIRED_LOGIN, PASSWORD_CHANGE, PROTECTED }

internal fun SessionState.rootDestination(): RootDestination = when (this) {
    SessionState.Restoring -> RootDestination.RESTORING
    SessionState.Unauthenticated -> RootDestination.LOGIN
    is SessionState.Expired -> RootDestination.EXPIRED_LOGIN
    is SessionState.Authenticated -> if (session.user?.requiresInitialPasswordChange == true) {
        RootDestination.PASSWORD_CHANGE
    } else {
        RootDestination.PROTECTED
    }
}

@Composable
fun AuthenticatedApp(
    sessionManager: SessionManager,
    loginApi: LoginApi,
    passwordApi: PasswordApi,
    companyApi: CompanyAccessApi? = null,
    companyStore: CompanyContextStore? = null,
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(sessionManager, loginApi, passwordApi, companyApi, companyStore),
    )
    val sessionState by authViewModel.sessionState.collectAsState()
    val companyState by authViewModel.companyState.collectAsState()
    val loginState by authViewModel.loginState.collectAsState()
    val recoveryState by authViewModel.recoveryState.collectAsState()
    val passwordChangeState by authViewModel.passwordChangeState.collectAsState()

    when (sessionState.rootDestination()) {
        RootDestination.RESTORING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        RootDestination.LOGIN, RootDestination.EXPIRED_LOGIN -> {
            val company = companyState.company
            if (company == null) CompanyAccessScreen(companyState, authViewModel::selectCompany, authViewModel::restoreCompanySelection)
            else key(company.companyId) {
                LoginScreen(
                    state = loginState, recoveryState = recoveryState,
                    sessionExpired = sessionState is SessionState.Expired,
                    onLogin = authViewModel::login, onRecoverPassword = authViewModel::requestPasswordRecovery,
                    company = company, onSwitchCompany = authViewModel::switchCompany,
                )
            }
        }
        RootDestination.PROTECTED -> key((sessionState as SessionState.Authenticated).session.user?.companyId) {
            val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
            DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                CameraPermissionHandler { AppNavigator(onLogout = authViewModel::logout) }
            }
        }
        RootDestination.PASSWORD_CHANGE -> ChangePasswordScreen(
            state = passwordChangeState,
            onChangePassword = authViewModel::changePassword,
            onLogout = authViewModel::logout,
        )
    }
}

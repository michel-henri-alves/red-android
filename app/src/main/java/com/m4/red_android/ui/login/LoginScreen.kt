package com.m4.red_android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.LoginFailure
import com.m4.red_android.viewmodels.LoginUiState
import com.m4.red_android.viewmodels.RecoveryFailure
import com.m4.red_android.viewmodels.RecoveryUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    recoveryState: RecoveryUiState,
    sessionExpired: Boolean,
    onLogin: (companyId: String, email: String, password: String) -> Unit,
    onRecoverPassword: (companyId: String, email: String) -> Unit,
    company: com.m4.red_android.auth.CompanyContext,
    onSwitchCompany: () -> Unit = {},
) {
    val companyId = company.companyId
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var recoveryOpen by remember { mutableStateOf(false) }
    val loading = state == LoginUiState.Loading

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Entrar em ${company.name}")
        TextButton(onClick = onSwitchCompany) { Text("Trocar empresa") }
        if (sessionExpired) Text("Sua sessão expirou. Entre novamente.")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            enabled = !loading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("login_email"),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            enabled = !loading,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("login_password"),
        )
        (state as? LoginUiState.Error)?.let { Text(it.failure.message()) }
        Button(
            onClick = { onLogin(companyId, email, password) },
            enabled = !loading && companyId.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp).testTag("login_submit"),
        ) {
            if (loading) CircularProgressIndicator() else Text("Entrar")
        }
        TextButton(onClick = { recoveryOpen = !recoveryOpen }) {
            Text("Esqueci minha senha")
        }
        if (recoveryOpen) {
            Text("Use o e-mail cadastrado nesta empresa.")
            Button(
                onClick = { onRecoverPassword(companyId, email) },
                enabled = recoveryState != RecoveryUiState.Loading &&
                    companyId.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.testTag("recovery_submit"),
            ) {
                Text(if (recoveryState == RecoveryUiState.Loading) "Solicitando..." else "Enviar senha temporária")
            }
            when (recoveryState) {
                RecoveryUiState.Accepted -> Text("Se os dados corresponderem a uma conta ativa, enviaremos uma senha temporária por e-mail.")
                is RecoveryUiState.Error -> Text(recoveryState.failure.message())
                else -> Unit
            }
        }
    }
}

private fun RecoveryFailure.message(): String = when (this) {
    RecoveryFailure.VALIDATION -> "Informe o e-mail."
    RecoveryFailure.THROTTLED -> "Muitas tentativas. Aguarde antes de tentar novamente."
    RecoveryFailure.CONNECTIVITY -> "Sem conexão. Verifique sua internet e tente novamente."
    RecoveryFailure.SERVER -> "Não foi possível solicitar a recuperação agora. Tente novamente."
}

private fun LoginFailure.message(): String = when (this) {
    LoginFailure.THROTTLED -> "Muitas tentativas. Aguarde antes de tentar novamente."
    LoginFailure.INVALID_CREDENTIALS -> "E-mail ou senha inválidos."
    LoginFailure.CONNECTIVITY -> "Sem conexão. Verifique sua internet e tente novamente."
    LoginFailure.SERVER -> "Não foi possível entrar agora. Tente novamente."
    LoginFailure.INVALID_RESPONSE -> "O servidor retornou uma sessão inválida."
}

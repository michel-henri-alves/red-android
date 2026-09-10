package com.m4.red_android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.PasswordChangeState

@Composable
fun ChangePasswordScreen(
    state: PasswordChangeState,
    onChangePassword: (String, String, String) -> Unit,
    onLogout: () -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val loading = state == PasswordChangeState.Loading

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Crie sua senha pessoal")
        Text("A senha temporária precisa ser substituída antes de continuar.")
        PasswordField("Senha temporária", "change_current", current, { current = it }, loading)
        PasswordField("Nova senha", "change_new", newPassword, { newPassword = it }, loading)
        PasswordField("Confirmar nova senha", "change_confirmation", confirmation, { confirmation = it }, loading)
        if (state is PasswordChangeState.Error) Text(state.message)
        Button(
            onClick = { onChangePassword(current, newPassword, confirmation) },
            enabled = !loading && current.isNotBlank() && newPassword.length >= 12 && newPassword == confirmation,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("change_submit"),
        ) {
            if (loading) CircularProgressIndicator() else Text("Alterar senha")
        }
        Button(onClick = onLogout, enabled = !loading) { Text("Sair") }
    }
}

@Composable
private fun PasswordField(
    label: String,
    testTag: String,
    value: String,
    onChange: (String) -> Unit,
    loading: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = !loading,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(testTag),
    )
}

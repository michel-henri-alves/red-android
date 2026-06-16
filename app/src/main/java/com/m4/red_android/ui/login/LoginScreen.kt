package com.m4.red_android.ui.login

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.m4.red_android.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel
) {
    Button(onClick = {
        viewModel.login("user", "pass")
        onLoginSuccess()
    }) {
        Text("Login")
    }
}
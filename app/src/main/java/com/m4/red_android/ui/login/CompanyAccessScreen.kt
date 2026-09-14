package com.m4.red_android.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.CompanyUiState

@Composable
fun CompanyAccessScreen(state: CompanyUiState, onResolve: (String) -> Unit, onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Qual é a sua empresa?")
        Text("Informe o nome de acesso fornecido pela empresa, como minha-loja.")
        OutlinedTextField(value = input, onValueChange = { input = it },
            label = { Text("Nome de acesso da empresa") }, singleLine = true,
            enabled = !state.restoring,
            modifier = Modifier.fillMaxWidth().testTag("company_access_name"))
        state.error?.let { Text(it) }
        Button(onClick = { onResolve(input) }, enabled = !state.restoring && input.isNotBlank(),
            modifier = Modifier.testTag("company_access_resolve")) {
            Text(if (state.loading) "Consultando..." else "Continuar")
        }
        TextButton(onClick = onBack, enabled = !state.restoring) { Text("Voltar") }
    }
}

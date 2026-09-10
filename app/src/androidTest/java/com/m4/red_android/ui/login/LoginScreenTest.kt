package com.m4.red_android.ui.login

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.m4.red_android.viewmodels.LoginFailure
import com.m4.red_android.viewmodels.LoginUiState
import com.m4.red_android.viewmodels.RecoveryUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun loginRequiresBothCredentialsAndSubmitsTypedValues() {
        var submitted: Triple<String, String, String>? = null
        composeRule.setContent {
            LoginScreen(
                LoginUiState.Idle,
                RecoveryUiState.Idle,
                sessionExpired = false,
                onLogin = { companyId, email, password -> submitted = Triple(companyId, email, password) },
                onRecoverPassword = { _, _ -> },
            )
        }

        composeRule.onNodeWithTag("login_submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("login_company_id").performTextInput("company-1")
        composeRule.onNodeWithTag("login_email").performTextInput("maria@example.com")
        composeRule.onNodeWithTag("login_password").performTextInput("secret")
        composeRule.onNodeWithTag("login_submit").assertIsEnabled().performClick()

        assertEquals(Triple("company-1", "maria@example.com", "secret"), submitted)
    }

    @Test fun expiredAndConnectivityStatesAreActionable() {
        composeRule.setContent {
            LoginScreen(
                LoginUiState.Error(LoginFailure.CONNECTIVITY),
                RecoveryUiState.Idle,
                sessionExpired = true,
                onLogin = { _, _, _ -> },
                onRecoverPassword = { _, _ -> },
            )
        }

        composeRule.onNodeWithText("Sua sessão expirou. Entre novamente.").assertIsDisplayed()
        composeRule.onNodeWithText("Sem conexão. Verifique sua internet e tente novamente.")
            .assertIsDisplayed()
    }

    @Test fun recoveryUsesTheTypedCompanyAndEmailAndShowsGenericFeedback() {
        var recovered: Pair<String, String>? = null
        composeRule.setContent {
            LoginScreen(
                LoginUiState.Idle,
                RecoveryUiState.Accepted,
                sessionExpired = false,
                onLogin = { _, _, _ -> },
                onRecoverPassword = { companyId, email -> recovered = companyId to email },
            )
        }

        composeRule.onNodeWithText("Esqueci minha senha").performClick()
        composeRule.onNodeWithTag("recovery_submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("login_company_id").performTextInput("company-1")
        composeRule.onNodeWithTag("login_email").performTextInput("maria@example.com")
        composeRule.onNodeWithTag("recovery_submit").assertIsEnabled().performClick()

        assertEquals("company-1" to "maria@example.com", recovered)
        composeRule.onNodeWithText(
            "Se os dados corresponderem a uma conta ativa, enviaremos uma senha temporária por e-mail.",
        ).assertIsDisplayed()
    }
}

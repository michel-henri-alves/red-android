package com.m4.red_android.ui.login

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.m4.red_android.viewmodels.PasswordChangeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChangePasswordScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun changeRequiresMatchingStrongLengthPasswordsAndSubmitsValues() {
        var submitted: Triple<String, String, String>? = null
        composeRule.setContent {
            ChangePasswordScreen(
                state = PasswordChangeState.Idle,
                onChangePassword = { current, newPassword, confirmation ->
                    submitted = Triple(current, newPassword, confirmation)
                },
                onLogout = {},
            )
        }

        composeRule.onNodeWithTag("change_submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("change_current").performTextInput("Temporary1!")
        composeRule.onNodeWithTag("change_new").performTextInput("NewPermanent1!")
        composeRule.onNodeWithTag("change_confirmation").performTextInput("NewPermanent1!")
        composeRule.onNodeWithTag("change_submit").assertIsEnabled().performClick()

        assertEquals(
            Triple("Temporary1!", "NewPermanent1!", "NewPermanent1!"),
            submitted,
        )
    }
}

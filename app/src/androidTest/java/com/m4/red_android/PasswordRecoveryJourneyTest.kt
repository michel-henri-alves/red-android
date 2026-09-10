package com.m4.red_android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.m4.red_android.auth.AuthenticatedSession
import com.m4.red_android.auth.SecureTokenStore
import com.m4.red_android.auth.SessionManager
import com.m4.red_android.auth.SessionState
import com.m4.red_android.data.api.NetworkEnvironment
import com.m4.red_android.data.api.RedNetworkClients
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in: disposable local MongoDB/Mailpit harness and adb reverse tcp:43801 tcp:43801. */
class PasswordRecoveryJourneyTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun recoveryEmailTemporaryLoginAndReplacementReachTheRealBackend() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("redRecoveryIntegration") == "true")
        val baseUrl = "http://localhost:43801/"
        // Prevent protected navigation's compatibility clients from reaching another environment.
        assertEquals(baseUrl, BuildConfig.API_BASE_URL)
        val manager = SessionManager(JourneyTokenStore())
        runBlocking { manager.restore() }
        val clients = RedNetworkClients(NetworkEnvironment(baseUrl, true), manager, false) { }
        val http = OkHttpClient()
        composeRule.setContent { AuthenticatedApp(manager, clients.loginApi, clients.passwordApi) }
        composeRule.onNodeWithTag("login_company_id").performTextInput("ui-company")
        composeRule.onNodeWithTag("login_email").performTextInput("android@example.com")
        composeRule.onNodeWithText("Esqueci minha senha").performClick()
        composeRule.onNodeWithTag("recovery_submit").performClick()

        var temporary: String? = null
        val deadline = System.currentTimeMillis() + 20000
        while (temporary == null && System.currentTimeMillis() < deadline) {
            http.newCall(Request.Builder().url("${baseUrl}__test/mail/android@example.com").build())
                .execute().use { response ->
                    if (response.isSuccessful) temporary = JSONObject(response.body!!.string()).getString("password")
                }
            if (temporary == null) Thread.sleep(250)
        }
        assertNotNull("The local worker must deliver the email", temporary)
        composeRule.onNodeWithTag("login_password").performTextInput(temporary!!)
        composeRule.onNodeWithTag("login_submit").performClick()
        composeRule.waitUntil(20000) { manager.state.value.rootDestination() == RootDestination.PASSWORD_CHANGE }
        val oldToken = (manager.state.value as SessionState.Authenticated).session.accessToken
        fun probe(token: String): Int = http.newCall(Request.Builder().url("${baseUrl}probe")
            .header("Authorization", "Bearer $token").build()).execute().use { it.code }
        assertEquals(403, probe(oldToken))
        composeRule.onNodeWithTag("change_current").performTextInput(temporary!!)
        composeRule.onNodeWithTag("change_new").performTextInput("AndroidReplacement123!")
        composeRule.onNodeWithTag("change_confirmation").performTextInput("AndroidReplacement123!")
        composeRule.onNodeWithTag("change_submit").performClick()
        composeRule.waitUntil(20000) { manager.state.value.rootDestination() == RootDestination.PROTECTED }
        val newToken = (manager.state.value as SessionState.Authenticated).session.accessToken
        assertEquals(200, probe(newToken))
        val staleChange = Request.Builder().url("${baseUrl}users/change-initial-password")
            .header("Authorization", "Bearer $oldToken")
            .post("{}".toRequestBody("application/json".toMediaType())).build()
        http.newCall(staleChange).execute().use { assertEquals(401, it.code) }
    }
}

private class JourneyTokenStore : SecureTokenStore {
    private var session: AuthenticatedSession? = null
    override suspend fun read(): AuthenticatedSession? = session
    override suspend fun write(session: AuthenticatedSession) { this.session = session }
    override suspend fun clear() { session = null }
}

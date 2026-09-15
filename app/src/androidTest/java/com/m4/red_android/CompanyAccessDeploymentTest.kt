package com.m4.red_android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import com.m4.red_android.auth.*
import com.m4.red_android.data.api.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Opt-in deployment smoke: public company lookup only; no credentials or email delivery. */
class CompanyAccessDeploymentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun deployedCompaniesResolveAndSwitchWithoutRetainingTypedCredentials() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("redCompanyDeployment") == "true")
        val session = SessionManager(object : SecureTokenStore {
            override suspend fun read(): AuthenticatedSession? = null
            override suspend fun write(session: AuthenticatedSession) = error("Unexpected authentication")
            override suspend fun clear() = Unit
        })
        runBlocking { session.restore() }
        val store = object : CompanyContextStore {
            var value: CompanyContext? = null
            override suspend fun read() = value
            override suspend fun write(company: CompanyContext) { value = company }
        }
        val resolver = Retrofit.Builder()
            .baseUrl("https://7700ezljb5.execute-api.us-east-1.amazonaws.com/")
            .addConverterFactory(GsonConverterFactory.create()).build().create(CompanyAccessApi::class.java)
        val login = object : LoginApi {
            override suspend fun login(request: LoginRequest): LoginResponse = error("Unexpected login")
            override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest): PasswordRecoveryResponse = error("Unexpected email")
        }
        val password = object : PasswordApi {
            override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse = error("Unexpected password change")
        }
        compose.setContent { AuthenticatedApp(session, login, password, resolver, store) }
        fun choose(accessName: String, displayName: String) {
            compose.waitUntil(20000) { compose.onAllNodesWithTag("company_access_name").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("company_access_name").performTextInput(accessName)
            compose.onNodeWithTag("company_access_resolve").performClick()
            compose.waitUntil(20000) { compose.onAllNodesWithText("Entrar em $displayName").fetchSemanticsNodes().isNotEmpty() }
        }
        choose("m4", "M4")
        compose.onNodeWithTag("login_email").performTextInput("unsent@example.com")
        compose.onNodeWithTag("login_password").performTextInput("not-submitted")
        compose.onNodeWithText("Trocar empresa").performClick()
        choose("ramon-lopes", "Ramon Lopes")
        for (tag in listOf("login_email", "login_password")) {
            assertEquals("", compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        }
        compose.onNodeWithTag("login_submit").assertIsNotEnabled()
        compose.onAllNodesWithText("unsent@example.com").assertCountEquals(0)
        assertNull(store.value)
    }
}

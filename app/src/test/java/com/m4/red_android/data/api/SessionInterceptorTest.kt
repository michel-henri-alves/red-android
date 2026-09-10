package com.m4.red_android.data.api

import com.m4.red_android.auth.AuthenticatedSession
import com.m4.red_android.auth.ExpirationReason
import com.m4.red_android.auth.FakeSecureTokenStore
import com.m4.red_android.auth.SessionManager
import com.m4.red_android.auth.SessionState
import java.util.Base64
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionInterceptorTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer() }
    @After fun tearDown() { server.shutdown() }

    @Test fun `protected request has exactly one current bearer credential`() = runTest {
        val manager = managerWith("current")
        server.enqueue(MockResponse())

        client(manager).newCall(
            Request.Builder().url(server.url("/sales"))
                .addHeader("Authorization", "Bearer stale")
                .build(),
        ).execute().close()

        assertEquals(listOf("Bearer current"), server.takeRequest().headers.values("Authorization"))
    }

    @Test fun `login request is explicitly unauthenticated`() = runTest {
        val manager = managerWith("secret")
        server.enqueue(MockResponse())

        client(manager).newCall(Request.Builder().url(server.url("/users/login")).build())
            .execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun `fresh response token replaces matching active token`() = runTest {
        val manager = managerWith("old", expiry = 2_000)
        val refreshed = jwt(expiry = 3_000)
        server.enqueue(MockResponse().addHeader("X-Access-Token", refreshed))

        client(manager).newCall(Request.Builder().url(server.url("/sales")).build()).execute().close()

        val session = (manager.state.value as SessionState.Authenticated).session
        assertEquals(refreshed, session.accessToken)
        assertEquals(3_000, session.expiresAtEpochSeconds)
    }

    @Test fun `matching 401 expires session without replay`() = runTest {
        val manager = managerWith("rejected")
        server.enqueue(MockResponse().setResponseCode(401))

        client(manager).newCall(Request.Builder().url(server.url("/sales")).build()).execute().close()

        assertEquals(SessionState.Expired(ExpirationReason.UNAUTHORIZED), manager.state.value)
        assertEquals(1, server.requestCount)
    }

    @Test fun `logger redacts credential headers and never logs login body`() {
        val logs = mutableListOf<String>()
        server.enqueue(MockResponse().addHeader("X-Access-Token", "renewed-secret"))
        val client = OkHttpClient.Builder()
            .addInterceptor(SafeHttpLoggingInterceptor(enabled = true, logger = logs::add))
            .build()
        val body = "{\"email\":\"person@example.com\",\"password\":\"body-secret\"}"
            .toRequestBody("application/json".toMediaType())

        client.newCall(
            Request.Builder().url(server.url("/users/login?password=query-secret"))
                .header("Authorization", "Bearer header-secret")
                .post(body)
                .build(),
        ).execute().close()

        val output = logs.joinToString("\n")
        assertFalse(output.contains("header-secret"))
        assertFalse(output.contains("renewed-secret"))
        assertFalse(output.contains("body-secret"))
        assertFalse(output.contains("query-secret"))
        assertFalse(output.contains("person@example.com"))
        assertTrue(output.contains("Authorization: ██"))
        assertTrue(output.contains("X-Access-Token: ██"))
    }

    private suspend fun managerWith(token: String, expiry: Long = 2_000): SessionManager =
        SessionManager(FakeSecureTokenStore(), epochSeconds = { 1_000 }).also {
            it.authenticate(AuthenticatedSession(token, expiry))
        }

    private fun client(manager: SessionManager) = OkHttpClient.Builder()
        .addInterceptor(SessionInterceptor(manager))
        .build()

    private fun jwt(expiry: Long): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf("{}", "{\"exp\":$expiry}", "signature")
            .joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }
}

package com.m4.red_android.data.api

import com.m4.red_android.auth.AuthenticatedSession
import com.m4.red_android.auth.FakeSecureTokenStore
import com.m4.red_android.auth.SessionManager
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RedNetworkClientsTest {
    private lateinit var server: MockWebServer
    private lateinit var manager: SessionManager

    @Before fun setUp() {
        server = MockWebServer()
        manager = SessionManager(FakeSecureTokenStore(), epochSeconds = { 1_000 })
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun `release-style environment rejects cleartext URL`() {
        val failure = runCatching {
            NetworkEnvironment("http://api.example.com/", cleartextAllowed = false)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test fun `debug-style environment may explicitly allow local cleartext URL`() {
        val environment = NetworkEnvironment(server.url("/").toString(), cleartextAllowed = true)

        assertTrue(environment.baseUrl.startsWith("http://"))
    }

    @Test fun `public login and authenticated endpoint use separate clients`() = runTest {
        manager.authenticate(AuthenticatedSession("active-token", 2_000))
        val clients = clients()
        server.enqueue(MockResponse())
        server.enqueue(MockResponse())

        clients.publicHttpClient.newCall(
            Request.Builder().url(server.url("/users/login")).build(),
        ).execute().close()
        clients.authenticatedHttpClient.newCall(
            Request.Builder().url(server.url("/sales")).build(),
        ).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer active-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test fun `login contract parses safe response through public Retrofit`() = runTest {
        val clients = clients()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"accessToken":"jwt","user":{"name":"Maria","role":"ADMIN","companyId":"company-1"}}""",
                ),
        )

        val response = clients.loginApi.login(LoginRequest("company-1", "maria@example.com", "secret"))

        assertEquals("jwt", response.accessToken)
        assertEquals(LoginUser("Maria", "ADMIN", "company-1"), response.user)
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun `password recovery sends tenant identity without authorization`() = runTest {
        val clients = clients()
        server.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"message":"password.recovery.request.accepted"}"""),
        )

        val response = clients.loginApi.requestPasswordRecovery(
            PasswordRecoveryRequest("company-1", "maria@example.com"),
        )

        assertEquals("password.recovery.request.accepted", response.message)
        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"companyId\":\"company-1\""))
    }

    @Test fun `release logging switch emits no network details`() {
        val logs = mutableListOf<String>()
        val clients = clients(loggingEnabled = false, logger = logs::add)
        server.enqueue(MockResponse())

        clients.publicHttpClient.newCall(
            Request.Builder().url(server.url("/health?password=query-secret")).build(),
        ).execute().close()

        assertTrue(logs.isEmpty())
    }

    private fun clients(
        loggingEnabled: Boolean = false,
        logger: (String) -> Unit = {},
    ) = RedNetworkClients(
        environment = NetworkEnvironment(server.url("/").toString(), cleartextAllowed = true),
        sessionManager = manager,
        loggingEnabled = loggingEnabled,
        logger = logger,
    )
}

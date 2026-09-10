package com.m4.red_android.data.api

import com.m4.red_android.auth.SessionManager
import com.m4.red_android.auth.SessionState
import com.google.gson.JsonParser
import java.util.Base64
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Applies the current credential and consumes the backend's sliding token response. */
class SessionInterceptor(
    private val sessionManager: SessionManager,
    private val isProtectedRequest: (okhttp3.Request) -> Boolean = {
        it.url.encodedPath != "/users/login"
    },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val active = if (isProtectedRequest(chain.request())) {
            (sessionManager.state.value as? SessionState.Authenticated)?.session
        } else {
            null
        }
        val request = if (active == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .removeHeader(AUTHORIZATION)
                .addHeader(AUTHORIZATION, "Bearer ${active.accessToken}")
                .build()
        }

        val response = chain.proceed(request)
        if (active == null) return response

        runBlocking {
            if (response.code == 401) {
                sessionManager.expireIfActive(active.accessToken)
            } else {
                response.header(REFRESHED_TOKEN)
                    ?.takeIf(String::isNotBlank)
                    ?.let { refreshed ->
                        JwtExpiryDecoder.expiry(refreshed)?.let { expiry ->
                            sessionManager.replaceToken(
                                expectedAccessToken = active.accessToken,
                                replacement = active.copy(
                                    accessToken = refreshed,
                                    expiresAtEpochSeconds = expiry,
                                ),
                            )
                        }
                    }
            }
        }
        return response
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val REFRESHED_TOKEN = "X-Access-Token"
    }
}

internal object JwtExpiryDecoder {
    fun expiry(token: String): Long? = runCatching {
        val payload = token.split('.').takeIf { it.size == 3 }?.get(1) ?: return null
        val decoded = Base64.getUrlDecoder().decode(payload)
        JsonParser.parseString(String(decoded, Charsets.UTF_8)).asJsonObject["exp"].asLong
    }.getOrNull()
}

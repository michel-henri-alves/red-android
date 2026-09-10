package com.m4.red_android.data.api

import okhttp3.Interceptor
import okhttp3.Response

/** Debug logging that deliberately never reads request or response bodies. */
class SafeHttpLoggingInterceptor(
    private val enabled: Boolean,
    private val logger: (String) -> Unit,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (enabled) {
            logger("--> ${request.method} ${request.url.withoutQuery()}")
            request.headers.forEach { (name, value) ->
                logger("$name: ${if (name.isSecret()) REDACTED else value}")
            }
        }
        val response = chain.proceed(request)
        if (enabled) {
            logger("<-- ${response.code} ${request.url.withoutQuery()}")
            response.headers.forEach { (name, value) ->
                logger("$name: ${if (name.isSecret()) REDACTED else value}")
            }
        }
        return response
    }

    private fun String.isSecret(): Boolean =
        equals(SessionInterceptor.AUTHORIZATION, ignoreCase = true) ||
            equals(SessionInterceptor.REFRESHED_TOKEN, ignoreCase = true)

    private fun okhttp3.HttpUrl.withoutQuery(): String = newBuilder().query(null).build().redact()

    companion object {
        private const val REDACTED = "██"
    }
}

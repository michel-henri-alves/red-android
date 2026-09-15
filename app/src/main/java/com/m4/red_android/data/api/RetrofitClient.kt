package com.m4.red_android.data.api

import com.m4.red_android.auth.SessionManager
import com.m4.red_android.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class NetworkEnvironment(
    val baseUrl: String,
    val cleartextAllowed: Boolean,
) {
    init {
        val parsed = baseUrl.toHttpUrl()
        require(baseUrl.endsWith('/')) { "API base URL must end with /" }
        require(parsed.isHttps || cleartextAllowed) { "Cleartext API URL is not allowed" }
    }
}

class RedNetworkClients(
    environment: NetworkEnvironment,
    sessionManager: SessionManager,
    loggingEnabled: Boolean,
    logger: (String) -> Unit,
) {
    internal val publicHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(SafeHttpLoggingInterceptor(loggingEnabled, logger))
        .build()

    internal val authenticatedHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(SessionInterceptor(sessionManager) { true })
        .addInterceptor(SafeHttpLoggingInterceptor(loggingEnabled, logger))
        .build()

    private val publicRetrofit = retrofit(environment.baseUrl, publicHttpClient)
    private val authenticatedRetrofit = retrofit(environment.baseUrl, authenticatedHttpClient)

    val companyAccessApi: CompanyAccessApi = publicRetrofit.create(CompanyAccessApi::class.java)
    val loginApi: LoginApi = publicRetrofit.create(LoginApi::class.java)
    val passwordApi: PasswordApi = authenticatedRetrofit.create(PasswordApi::class.java)
    val productApi: ProductApi = authenticatedRetrofit.create(ProductApi::class.java)
    val salesApi: SalesApi = authenticatedRetrofit.create(SalesApi::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

/** Compatibility facade for current view models; initialized once by [com.m4.red_android.RedApplication]. */
object RetrofitClient {
    @Volatile private var configured: RedNetworkClients? = null

    fun initialize(sessionManager: SessionManager, logger: (String) -> Unit) {
        if (configured != null) return
        synchronized(this) {
            if (configured == null) {
                configured = RedNetworkClients(
                    environment = NetworkEnvironment(
                        baseUrl = BuildConfig.API_BASE_URL,
                        cleartextAllowed = BuildConfig.CLEARTEXT_API_ALLOWED,
                    ),
                    sessionManager = sessionManager,
                    loggingEnabled = BuildConfig.NETWORK_LOGGING_ENABLED,
                    logger = logger,
                )
            }
        }
    }

    val companyAccessApi: CompanyAccessApi get() = clients().companyAccessApi
    val loginApi: LoginApi get() = clients().loginApi
    val passwordApi: PasswordApi get() = clients().passwordApi
    val productApi: ProductApi get() = clients().productApi
    val salesApi: SalesApi get() = clients().salesApi

    private fun clients(): RedNetworkClients =
        checkNotNull(configured) { "RetrofitClient must be initialized by RedApplication" }
}

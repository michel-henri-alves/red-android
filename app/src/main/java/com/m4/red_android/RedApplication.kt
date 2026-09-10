package com.m4.red_android

import android.app.Application
import com.m4.red_android.auth.KeystoreSecureTokenStore
import com.m4.red_android.auth.SessionManager
import com.m4.red_android.data.api.RetrofitClient
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RedApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val sessionManager: SessionManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SessionManager(KeystoreSecureTokenStore(this))
    }

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(sessionManager) { message -> Log.d("RedNetwork", message) }
        applicationScope.launch { sessionManager.restore() }
    }
}

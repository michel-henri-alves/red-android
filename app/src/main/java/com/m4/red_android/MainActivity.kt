package com.m4.red_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.m4.red_android.data.api.RetrofitClient
import com.m4.red_android.ui.theme.AppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val redApplication = application as RedApplication
                AuthenticatedApp(
                    redApplication.sessionManager,
                    RetrofitClient.loginApi,
                    RetrofitClient.passwordApi,
                    RetrofitClient.companyAccessApi,
                    redApplication.companyContextStore,
                )
            }
        }
    }
}

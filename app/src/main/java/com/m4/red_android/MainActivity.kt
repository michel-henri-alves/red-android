package com.m4.red_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.m4.red_android.ui.permissions.CameraPermissionHandler


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                CameraPermissionHandler {
                    AppNavigator()
                }
            }
        }
    }
}
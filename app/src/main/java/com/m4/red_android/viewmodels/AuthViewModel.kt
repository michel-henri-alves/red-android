package com.m4.red_android.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {

    var isLoggedIn by mutableStateOf(false)
        private set

    fun login(username: String, password: String) {
        // chamar API aqui
        isLoggedIn = true
    }

    fun logout() {
        isLoggedIn = false
    }
}
package com.m4.red_android.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApi {
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("users/password-recovery")
    suspend fun requestPasswordRecovery(@Body request: PasswordRecoveryRequest): PasswordRecoveryResponse
}

data class LoginRequest(
    val companyId: String,
    val email: String,
    val password: String,
)

data class LoginResponse(
    val accessToken: String,
    val user: LoginUser,
)

data class LoginUser(
    val name: String,
    val role: String,
    val companyId: String,
    val requiresInitialPasswordChange: Boolean = false,
)

data class PasswordRecoveryRequest(
    val companyId: String,
    val email: String,
)

data class PasswordRecoveryResponse(
    val message: String,
)

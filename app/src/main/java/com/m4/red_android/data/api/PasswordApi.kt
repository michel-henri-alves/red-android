package com.m4.red_android.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface PasswordApi {
    @POST("users/change-initial-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ChangePasswordResponse
}

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
)

data class ChangePasswordResponse(val message: String)

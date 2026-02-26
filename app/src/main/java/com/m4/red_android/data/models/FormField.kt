package com.m4.red_android.data.models

data class FormField<T>(
    val text: String = "",
    val value: T? = null,
    val error: String? = null
)
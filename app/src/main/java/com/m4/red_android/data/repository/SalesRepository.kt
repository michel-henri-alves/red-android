package com.m4.red_android.data.repository

import com.m4.red_android.sales.SaleSnapshot

sealed interface SaleSubmissionResult {
    data object Success : SaleSubmissionResult
    data class Failure(val cause: Throwable) : SaleSubmissionResult
}

interface SalesRepository {
    suspend fun submit(snapshot: SaleSnapshot): SaleSubmissionResult
}

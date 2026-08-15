package com.m4.red_android.sales

sealed interface SaleSubmissionState {
    data object Ready : SaleSubmissionState
    data class Submitting(val snapshot: SaleSnapshot) : SaleSubmissionState
    data class Succeeded(val snapshot: SaleSnapshot) : SaleSubmissionState
    data class Failed(
        val snapshot: SaleSnapshot,
        val error: SaleSubmissionError,
    ) : SaleSubmissionState
}

data class SaleSubmissionError(
    val message: String,
    val isRetryable: Boolean = true,
)

sealed interface SaleSubmissionEffect {
    data class Completed(val submissionId: String) : SaleSubmissionEffect
}

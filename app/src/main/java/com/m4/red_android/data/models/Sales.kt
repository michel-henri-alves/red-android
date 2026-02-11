package com.m4.red_android.data.models

import com.m4.red_android.data.enums.PaymentMethod

data class Sales (
//    val paymentMethod: MutableList<PaymentMethod>? = emptyList(),
//    val amountPaid: MutableList<Double>? = emptyList(),
//    val discount: Double,
//    val change: Double,
//    val vendor: String,
//    val realizedAt: LocalDateTime?
    val paymentMethod: List<PaymentMethod>,
    val amountPaid: MutableList<Double?>?,
    val discount: Double,
    val change: Double,
    val vendor: String,
    val realizedAt: String
)
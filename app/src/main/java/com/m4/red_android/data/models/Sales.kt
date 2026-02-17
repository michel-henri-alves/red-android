package com.m4.red_android.data.models

import com.m4.red_android.data.enums.PaymentMethod

data class Sales (
    val code: String,
    val items: List<Item>,
    val paymentMethod: List<PaymentMethod>,
    val amountPaid: MutableList<Double?>?,
    val discount: Double,
    val change: Double,
    val vendor: String,
    val realizedAt: String
)
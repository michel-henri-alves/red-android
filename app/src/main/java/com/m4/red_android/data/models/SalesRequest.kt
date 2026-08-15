package com.m4.red_android.data.models

import com.m4.red_android.data.enums.PaymentMethod

data class SaleItemRequest(
    val smartCode: String,
    val quantity: String,
    val productName: String,
    val unitOfMeasurement: String,
    val price: Double,
)

data class SalesRequest(
    val items: List<SaleItemRequest>,
    val paymentMethod: List<PaymentMethod>,
    val amountPaid: List<Double>,
    val discount: Double,
    val change: Double,
    val vendor: String,
    val realizedAt: String,
)

package com.m4.red_android.data.validation

data class ProductFormValidation(
    val smartCode: String? = null,
    val name: String? = null,
    val unitOfMeasurement: String? = null,
    val priceForSale: String? = null,
    val actualQuantity: String? = null
)

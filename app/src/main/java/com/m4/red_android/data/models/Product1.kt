package com.m4.red_android.data.models

import com.m4.red_android.data.enums.UnitOfMeasurement

data class Product1(
    val smartCode: String?,
    val name: String?,
    val manufacturer: String?,
    val category: String?,
    val ownManufacture: Boolean?,
    val priceForSale: Double?,
    val minQuantity: Int?,
    val actualQuantity: Int?,
    val unitOfMeasurement: UnitOfMeasurement?,
    val code: String
)

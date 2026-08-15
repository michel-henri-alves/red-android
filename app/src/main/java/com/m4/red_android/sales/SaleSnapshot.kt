package com.m4.red_android.sales

import com.m4.red_android.data.enums.PaymentMethod
import com.m4.red_android.data.models.Item

data class SaleItemSnapshot(
    val smartCode: String,
    val quantity: String,
    val productName: String,
    val unitOfMeasurement: String,
    val price: Money,
    val code: String,
)

data class SaleSnapshot(
    val submissionId: String,
    val code: String,
    val items: List<SaleItemSnapshot>,
    val payments: List<Payment>,
    val discount: Money,
    val change: Money,
    val vendor: String,
    val realizedAt: String,
) {
    init {
        require(submissionId.isNotBlank()) { "Submission id is required" }
        require(code.isNotBlank()) { "Sale code is required" }
        require(items.isNotEmpty()) { "At least one sale item is required" }
        require(payments.isNotEmpty()) { "At least one payment is required" }
        require(vendor.isNotBlank()) { "Vendor is required" }
        require(realizedAt.isNotBlank()) { "Realization timestamp is required" }
    }

    companion object {
        fun create(
            submissionId: String,
            code: String,
            items: List<Item>,
            paymentMethods: List<PaymentMethod>,
            amountsPaid: List<Double>,
            discount: Double,
            change: Double,
            vendor: String,
            realizedAt: String,
        ): SaleSnapshot {
            require(paymentMethods.size == amountsPaid.size) {
                "Each payment method must have one amount"
            }

            return create(
                submissionId = submissionId,
                code = code,
                items = items,
                payments = paymentMethods.zip(amountsPaid).map { (method, amount) ->
                    Payment(method, Money.fromLegacyDouble(amount))
                },
                discount = Money.fromLegacyDouble(discount),
                change = Money.fromLegacyDouble(change),
                vendor = vendor,
                realizedAt = realizedAt,
            )
        }

        fun create(
            submissionId: String,
            code: String,
            items: List<Item>,
            payments: List<Payment>,
            discount: Money,
            change: Money,
            vendor: String,
            realizedAt: String,
        ): SaleSnapshot = SaleSnapshot(
                submissionId = submissionId,
                code = code,
                items = items.map { item ->
                    SaleItemSnapshot(
                        smartCode = item.smartCode,
                        quantity = item.quantity,
                        productName = item.productName,
                        unitOfMeasurement = item.unitOfMeasurement,
                        price = Money.fromLegacyDouble(item.price),
                        code = item.code,
                    )
                }.toList(),
                payments = payments.toList(),
                discount = discount,
                change = change,
                vendor = vendor,
                realizedAt = realizedAt,
            )
    }
}

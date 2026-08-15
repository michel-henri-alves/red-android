package com.m4.red_android.sales

import com.m4.red_android.data.enums.PaymentMethod

data class Payment(
    val method: PaymentMethod,
    val amount: Money,
) {
    init {
        require(amount.cents > 0) { "Payment amount must be positive" }
    }

    companion object {
        fun fromInput(method: PaymentMethod?, amount: String): Payment {
            requireNotNull(method) { "Payment method is required" }
            return Payment(method, Money.parseDecimal(amount))
        }
    }
}

data class SaleCalculation(
    val total: Money,
    val discount: Money,
    val netTotal: Money,
    val paid: Money,
    val balance: Money,
    val change: Money,
    val isComplete: Boolean,
)

object SaleCalculator {

    fun calculate(
        total: Money,
        discount: Money,
        payments: List<Payment>,
    ): SaleCalculation {
        require(discount.cents <= total.cents) { "Discount cannot exceed sale total" }

        val netTotalCents = Math.subtractExact(total.cents, discount.cents)
        val paidCents = payments.fold(0L) { accumulated, payment ->
            Math.addExact(accumulated, payment.amount.cents)
        }

        val isComplete = paidCents >= netTotalCents
        val balanceCents = if (isComplete) {
            0L
        } else {
            Math.subtractExact(netTotalCents, paidCents)
        }
        val changeCents = if (isComplete) {
            Math.subtractExact(paidCents, netTotalCents)
        } else {
            0L
        }

        return SaleCalculation(
            total = total,
            discount = discount,
            netTotal = Money.fromCents(netTotalCents),
            paid = Money.fromCents(paidCents),
            balance = Money.fromCents(balanceCents),
            change = Money.fromCents(changeCents),
            isComplete = isComplete,
        )
    }
}

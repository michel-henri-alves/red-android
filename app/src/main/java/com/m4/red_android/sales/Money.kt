package com.m4.red_android.sales

import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class Money private constructor(val cents: Long) {

    fun toLegacyDouble(): Double = BigDecimal.valueOf(cents, 2).toDouble()

    companion object {
        val ZERO: Money = Money(0)

        fun fromCents(cents: Long): Money {
            require(cents >= 0) { "Money cannot be negative" }
            return Money(cents)
        }

        fun parseDecimal(input: String): Money {
            val normalized = input.trim().replace(',', '.')
            require(DECIMAL_PATTERN.matches(normalized)) {
                "Amount must be a non-negative decimal with at most two fractional digits"
            }

            return try {
                val cents = BigDecimal(normalized)
                    .movePointRight(2)
                    .longValueExact()
                fromCents(cents)
            } catch (error: ArithmeticException) {
                throw IllegalArgumentException("Amount is outside the supported cent range", error)
            }
        }

        fun fromLegacyDouble(value: Double): Money {
            require(value.isFinite()) { "Amount must be finite" }
            require(value >= 0.0) { "Money cannot be negative" }

            return try {
                val cents = BigDecimal.valueOf(value)
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact()
                fromCents(cents)
            } catch (error: ArithmeticException) {
                throw IllegalArgumentException(
                    "Legacy amount must be representable as whole cents",
                    error,
                )
            }
        }

        private val DECIMAL_PATTERN = Regex("\\d+(?:\\.\\d{1,2})?")
    }
}

package com.m4.red_android.sales

import com.m4.red_android.data.enums.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleCalculatorTest {

    @Test
    fun parsesDotCommaAndSingleDigitFractionsToExactCents() {
        assertEquals(1_050L, Money.parseDecimal("10.50").cents)
        assertEquals(1_050L, Money.parseDecimal("10,50").cents)
        assertEquals(1_050L, Money.parseDecimal("10.5").cents)
        assertEquals(1_000L, Money.parseDecimal("10").cents)
    }

    @Test
    fun exactPaymentCompletesWithoutBalanceOrChange() {
        val result = SaleCalculator.calculate(
            total = money("100.00"),
            discount = Money.ZERO,
            payments = listOf(payment(PaymentMethod.PIX, "100.00")),
        )

        assertEquals(10_000L, result.paid.cents)
        assertEquals(0L, result.balance.cents)
        assertEquals(0L, result.change.cents)
        assertTrue(result.isComplete)
    }

    @Test
    fun partialPaymentsAccumulateWithoutLosingCents() {
        val result = SaleCalculator.calculate(
            total = money("100.00"),
            discount = Money.ZERO,
            payments = listOf(
                payment(PaymentMethod.CASH, "30.10"),
                payment(PaymentMethod.DEBIT, "20.20"),
            ),
        )

        assertEquals(5_030L, result.paid.cents)
        assertEquals(4_970L, result.balance.cents)
        assertEquals(0L, result.change.cents)
        assertFalse(result.isComplete)
    }

    @Test
    fun excessPaymentProducesChangeAndNeverNegativeBalance() {
        val result = SaleCalculator.calculate(
            total = money("80.00"),
            discount = Money.ZERO,
            payments = listOf(payment(PaymentMethod.CASH, "100.00")),
        )

        assertEquals(0L, result.balance.cents)
        assertEquals(2_000L, result.change.cents)
        assertTrue(result.isComplete)
    }

    @Test
    fun discountReducesAmountDueBeforePayments() {
        val result = SaleCalculator.calculate(
            total = money("100.00"),
            discount = money("10.01"),
            payments = listOf(payment(PaymentMethod.CREDIT, "89.99")),
        )

        assertEquals(8_999L, result.netTotal.cents)
        assertEquals(0L, result.balance.cents)
        assertEquals(0L, result.change.cents)
        assertTrue(result.isComplete)
    }

    @Test
    fun rejectsDiscountGreaterThanTotal() {
        assertThrows(IllegalArgumentException::class.java) {
            SaleCalculator.calculate(
                total = money("10.00"),
                discount = money("10.01"),
                payments = emptyList(),
            )
        }
    }

    @Test
    fun rejectsMissingMethodAndNonPositivePayment() {
        assertThrows(IllegalArgumentException::class.java) {
            Payment.fromInput(method = null, amount = "10.00")
        }
        assertThrows(IllegalArgumentException::class.java) {
            Payment.fromInput(method = PaymentMethod.CASH, amount = "0")
        }
        assertThrows(IllegalArgumentException::class.java) {
            Payment.fromInput(method = PaymentMethod.CASH, amount = "-1.00")
        }
    }

    @Test
    fun rejectsMalformedOrFractionalCentInput() {
        listOf("", " ", "abc", "1.2.3", "1,2,3", "1.001", "1,001", "NaN", "Infinity")
            .forEach { invalid ->
                assertThrows("Expected '$invalid' to be rejected", IllegalArgumentException::class.java) {
                    Money.parseDecimal(invalid)
                }
            }
    }

    @Test
    fun rejectsNegativeMoneyAndNonFiniteOrFractionalCentLegacyValues() {
        assertThrows(IllegalArgumentException::class.java) { Money.fromCents(-1) }
        assertThrows(IllegalArgumentException::class.java) { Money.fromLegacyDouble(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) {
            Money.fromLegacyDouble(Double.POSITIVE_INFINITY)
        }
        assertThrows(IllegalArgumentException::class.java) { Money.fromLegacyDouble(10.001) }
    }

    @Test
    fun legacyDecimalConversionDoesNotExposeBinaryFloatingPointNoise() {
        assertEquals(10L, Money.fromLegacyDouble(0.10).cents)
        assertEquals(20L, Money.fromLegacyDouble(0.20).cents)

        val result = SaleCalculator.calculate(
            total = Money.fromLegacyDouble(0.30),
            discount = Money.ZERO,
            payments = listOf(
                Payment(PaymentMethod.CASH, Money.fromLegacyDouble(0.10)),
                Payment(PaymentMethod.PIX, Money.fromLegacyDouble(0.20)),
            ),
        )

        assertEquals(0L, result.balance.cents)
        assertTrue(result.isComplete)
    }

    @Test
    fun mapsWholeCentsToTheExistingLegacyNumericBoundary() {
        assertEquals(10.01, money("10.01").toLegacyDouble(), 0.0)
        assertEquals(0.0, Money.ZERO.toLegacyDouble(), 0.0)
    }

    @Test
    fun rejectsOverflowWhenAccumulatingPayments() {
        assertThrows(ArithmeticException::class.java) {
            SaleCalculator.calculate(
                total = Money.fromCents(Long.MAX_VALUE),
                discount = Money.ZERO,
                payments = listOf(
                    Payment(PaymentMethod.CASH, Money.fromCents(Long.MAX_VALUE)),
                    Payment(PaymentMethod.PIX, Money.fromCents(1)),
                ),
            )
        }
    }

    private fun money(value: String): Money = Money.parseDecimal(value)

    private fun payment(method: PaymentMethod, value: String): Payment =
        Payment.fromInput(method, value)
}

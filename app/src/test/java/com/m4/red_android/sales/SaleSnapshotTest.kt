package com.m4.red_android.sales

import com.m4.red_android.data.enums.PaymentMethod
import com.m4.red_android.data.models.Item
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaleSnapshotTest {

    @Test
    fun capturesEveryPayloadFieldBeforeMutableDraftListsAreReset() = runTest {
        val items = mutableListOf(
            item(code = "P-1", price = 10.01),
            item(code = "P-2", price = 20.02),
        )
        val methods = mutableListOf(PaymentMethod.CASH, PaymentMethod.PIX)
        val amounts = mutableListOf(15.00, 14.03)
        val snapshot = SaleSnapshot.create(
            submissionId = "submission-123",
            code = "sale-1",
            items = items,
            paymentMethods = methods,
            amountsPaid = amounts,
            discount = 1.00,
            change = 0.00,
            vendor = "app",
            realizedAt = "2026-08-15T10:30:00",
        )

        items.clear()
        methods.clear()
        amounts.clear()

        val repository = FakeSalesRepository()
        repository.submit(snapshot)

        val submitted = repository.submittedSnapshots.single()
        assertEquals("submission-123", submitted.submissionId)
        assertEquals("sale-1", submitted.code)
        assertEquals(listOf("P-1", "P-2"), submitted.items.map { it.code })
        assertEquals(listOf(1_001L, 2_002L), submitted.items.map { it.price.cents })
        assertEquals(
            listOf(PaymentMethod.CASH, PaymentMethod.PIX),
            submitted.payments.map { it.method },
        )
        assertEquals(listOf(1_500L, 1_403L), submitted.payments.map { it.amount.cents })
        assertEquals(100L, submitted.discount.cents)
        assertEquals(0L, submitted.change.cents)
        assertEquals("app", submitted.vendor)
        assertEquals("2026-08-15T10:30:00", submitted.realizedAt)
    }

    @Test
    fun rejectsMismatchedPaymentMethodsAndAmounts() {
        assertThrows(IllegalArgumentException::class.java) {
            SaleSnapshot.create(
                submissionId = "submission-123",
                code = "sale-1",
                items = listOf(item(code = "P-1", price = 10.00)),
                paymentMethods = listOf(PaymentMethod.CASH),
                amountsPaid = emptyList(),
                discount = 0.0,
                change = 0.0,
                vendor = "app",
                realizedAt = "2026-08-15T10:30:00",
            )
        }
    }

    private fun item(code: String, price: Double) = Item(
        smartCode = "smart-$code",
        quantity = "1",
        productName = "Product $code",
        unitOfMeasurement = "UN",
        price = price,
        code = code,
    )
}

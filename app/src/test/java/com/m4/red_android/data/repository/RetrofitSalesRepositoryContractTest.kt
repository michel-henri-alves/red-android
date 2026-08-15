package com.m4.red_android.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.m4.red_android.data.enums.PaymentMethod
import com.m4.red_android.data.models.Item
import com.m4.red_android.sales.SaleSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrofitSalesRepositoryContractTest {

    @Test
    fun serializesTheBackendPostSalesRequestWithoutLegacyOrTenantFields() {
        val request = snapshot().toApiModel()
        val json = JsonParser.parseString(Gson().toJson(request)).asJsonObject

        assertEquals(
            setOf(
                "items",
                "paymentMethod",
                "amountPaid",
                "discount",
                "change",
                "vendor",
                "realizedAt",
            ),
            json.keySet(),
        )
        assertFalse(json.has("code"))
        assertFalse(json.has("companyId"))

        val item = json.getAsJsonArray("items").single().asJsonObject
        assertEquals(
            setOf("smartCode", "quantity", "productName", "unitOfMeasurement", "price"),
            item.keySet(),
        )
        assertFalse(item.has("code"))
        assertFalse(item.has("companyId"))

        assertEquals("PIX", json.getAsJsonArray("paymentMethod").single().asString)
        assertEquals(10.01, json.getAsJsonArray("amountPaid").single().asDouble, 0.0)
        assertEquals(0.01, json.get("discount").asDouble, 0.0)
        assertEquals(0.0, json.get("change").asDouble, 0.0)
        assertEquals("2026-08-15T10:30:00-03:00", json.get("realizedAt").asString)
        assertTrue(json.get("amountPaid").asJsonArray.all { it.isJsonPrimitive })
    }

    private fun snapshot() = SaleSnapshot.create(
        submissionId = "submission-1",
        code = "local-sale-code",
        items = listOf(
            Item(
                smartCode = "SKU-1",
                quantity = "1",
                productName = "Coffee",
                unitOfMeasurement = "UNIT",
                price = 10.0,
                code = "local-item-code",
            ),
        ),
        paymentMethods = listOf(PaymentMethod.PIX),
        amountsPaid = listOf(10.01),
        discount = 0.01,
        change = 0.0,
        vendor = "app",
        realizedAt = "2026-08-15T10:30:00-03:00",
    )
}

package com.m4.red_android.data.repository

import com.m4.red_android.data.api.SalesApi
import com.m4.red_android.data.models.SaleItemRequest
import com.m4.red_android.data.models.SalesRequest
import com.m4.red_android.sales.SaleSnapshot
import kotlinx.coroutines.CancellationException

class RetrofitSalesRepository(
    private val salesApi: SalesApi,
) : SalesRepository {

    override suspend fun submit(snapshot: SaleSnapshot): SaleSubmissionResult = try {
        salesApi.postSales(snapshot.toApiModel())
        SaleSubmissionResult.Success
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: Exception) {
        SaleSubmissionResult.Failure(cause)
    }
}

internal fun SaleSnapshot.toApiModel(): SalesRequest = SalesRequest(
    items = items.map { item ->
        SaleItemRequest(
            smartCode = item.smartCode,
            quantity = item.quantity,
            productName = item.productName,
            unitOfMeasurement = item.unitOfMeasurement,
            price = item.price.toLegacyDouble(),
        )
    },
    paymentMethod = payments.map { it.method },
    amountPaid = payments.map { payment -> payment.amount.toLegacyDouble() },
    discount = discount.toLegacyDouble(),
    change = change.toLegacyDouble(),
    vendor = vendor,
    realizedAt = realizedAt,
)

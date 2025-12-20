package com.m4.red_android.data.api

import com.m4.red_android.data.models.Product
import com.m4.red_android.data.models.Sales
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface SalesApi {

    @POST("sales")
    suspend fun postSales(
        @Body sale: Sales
    )
}

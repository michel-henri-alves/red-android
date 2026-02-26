package com.m4.red_android.data.api

import com.m4.red_android.data.models.Product
import com.m4.red_android.data.models.Product1
import com.m4.red_android.data.models.Sales
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response


interface ProductApi {

    @GET("products/by-smartcode/{smartCode}")
    suspend fun getProduct(
        @Path("smartCode") smartCode: String
    ): Product

    @POST("products")
    suspend fun postProduct(
        @Body product: Product1
    ): Response<Product1>
}

package com.m4.red_android.data.api

import com.m4.red_android.data.models.Product
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {

    @GET("products/by-smartcode/{smartCode}")
    suspend fun getProduct(
        @Path("smartCode") smartCode: String
    ): Product
}

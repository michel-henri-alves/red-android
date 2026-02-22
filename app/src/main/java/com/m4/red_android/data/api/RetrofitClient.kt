package com.m4.red_android.data.api

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {

//    private const val BASE_URL = "https://1biotj4t46.execute-api.us-east-1.amazonaws.com"
private const val BASE_URL = "http://192.168.1.167:3001"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

//    val api: ProductApi = Retrofit.Builder()
//        .baseUrl(BASE_URL)
//        .client(client)
//        .addConverterFactory(GsonConverterFactory.create()) // <-- MUDANÇA
//        .build()
//        .create(ProductApi::class.java)

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val productApi: ProductApi = retrofit.create(ProductApi::class.java)
    val salesApi: SalesApi = retrofit.create(SalesApi::class.java)
}
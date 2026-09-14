package com.m4.red_android.data.api

import com.m4.red_android.auth.CompanyContext
import retrofit2.http.Body
import retrofit2.http.POST

interface CompanyAccessApi {
    @POST("companies/resolve-access")
    suspend fun resolve(@Body request: CompanyAccessRequest): CompanyAccessResponse
}
data class CompanyAccessRequest(val accessName: String)

data class CompanyAccessResponse(val companyId: String, val accessName: String, val name: String) {
    fun toContext() = CompanyContext(companyId, accessName, name)
}

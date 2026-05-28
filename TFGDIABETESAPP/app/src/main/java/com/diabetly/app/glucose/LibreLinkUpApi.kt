package com.diabetly.app.glucose

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface LibreLinkUpApi {

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("llu/auth/login")
    suspend fun login(
        @Header("version") version: String = "4.16.0",
        @Header("product") product: String = "llu.android",
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @GET("llu/connections")
    suspend fun getConnections(
        @Header("version") version: String = "4.16.0",
        @Header("product") product: String = "llu.android",
        @Header("Authorization") token: String,
        @Header("account-id") accountId: String
    ): Response<ConnectionsResponse>

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @GET("llu/connections/{patientId}/graph")
    suspend fun getGlucoseGraph(
        @Header("version") version: String = "4.16.0",
        @Header("product") product: String = "llu.android",
        @Header("Authorization") token: String,
        @Header("account-id") accountId: String,
        @Path("patientId") patientId: String
    ): Response<GlucoseResponse>
}
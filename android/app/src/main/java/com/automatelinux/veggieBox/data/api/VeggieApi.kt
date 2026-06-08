package com.automatelinux.veggieBox.data.api

import com.automatelinux.veggieBox.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface VeggieApi {
    @GET("api/route/today")
    suspend fun route(@Query("worker") worker: Int? = null): RouteResponse

    @GET("api/earnings")
    suspend fun earnings(@Query("worker") worker: Int? = null): Earnings

    @GET("api/greeting")
    suspend fun greeting(@Query("all") all: Int? = null): GreetResponse

    @PATCH("api/stops/{id}")
    suspend fun updateStop(@Path("id") id: Int, @Body body: StatusBody): SimpleOk

    @POST("api/stops/{id}/on-my-way")
    suspend fun onMyWay(@Path("id") id: Int): OnMyWayResponse

    @POST("api/route/optimize")
    suspend fun optimize(@Body body: OptimizeBody): SimpleOk

    @PATCH("api/customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): SimpleOk

    @Multipart
    @POST("api/stops/{id}/media")
    suspend fun uploadMedia(@Path("id") id: Int, @Part file: MultipartBody.Part): SimpleOk
}

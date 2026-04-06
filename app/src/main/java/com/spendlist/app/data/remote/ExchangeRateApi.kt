package com.spendlist.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): ExchangeRateResponse
}

@Serializable
data class ExchangeRateResponse(
    val result: String,
    @SerialName("base_code")
    val baseCode: String,
    val rates: Map<String, Double>
)

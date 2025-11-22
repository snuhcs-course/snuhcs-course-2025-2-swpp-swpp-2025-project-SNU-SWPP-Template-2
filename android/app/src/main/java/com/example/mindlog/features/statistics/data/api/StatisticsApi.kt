package com.example.mindlog.features.statistics.data.api

import com.example.mindlog.features.statistics.data.dto.EmotionRatesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface StatisticsApi {
    @GET("statistics/emotion-rate")
    suspend fun getEmotionRates(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
    ): EmotionRatesResponse
}
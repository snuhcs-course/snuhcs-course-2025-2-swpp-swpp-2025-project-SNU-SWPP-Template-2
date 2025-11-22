package com.example.mindlog.features.analysis.data.api

import com.example.mindlog.features.analysis.data.dto.ComprehensiveAnalysisResponse
import com.example.mindlog.features.analysis.data.dto.PersonalizedAdviceResponse
import com.example.mindlog.features.analysis.data.dto.UserTypeResponse
import retrofit2.http.GET
import retrofit2.http.PATCH

interface AnalysisApi {

    @GET("analysis/user-type")
    suspend fun getUserType(): UserTypeResponse

    @GET("analysis/comprehensive-analysis")
    suspend fun getComprehensiveAnalysis(): ComprehensiveAnalysisResponse

    @GET("analysis/personalized-advice")
    suspend fun getPersonalizedAdvice(): PersonalizedAdviceResponse
}
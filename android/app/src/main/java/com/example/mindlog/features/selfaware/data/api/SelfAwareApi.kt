package com.example.mindlog.features.selfaware.data.api

import com.example.mindlog.features.selfaware.data.dto.AnswerRequest
import com.example.mindlog.features.selfaware.data.dto.AnswerResponse
import com.example.mindlog.features.selfaware.data.dto.QACursorResponse
import com.example.mindlog.features.selfaware.data.dto.QAResponse
import com.example.mindlog.features.selfaware.data.dto.TopValueScoresResponse
import com.example.mindlog.features.selfaware.data.dto.ValueMapResponse
import com.example.mindlog.features.selfaware.data.dto.ValueScoreResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SelfAwareApi {
    @GET("self-aware/question")
    suspend fun getTodayQA(@Query("date") date: String): QAResponse

    @POST("self-aware/answer")
    suspend fun submitAnswer(@Body answerRequest: AnswerRequest): AnswerResponse

    @GET("self-aware/QA-history")
    suspend fun getQAHistory(@Query("limit") limit: Int, @Query("cursor") cursor: Int?): QACursorResponse

    @GET("self-aware/value-map")
    suspend fun getValueMap(): ValueMapResponse

    @GET("self-aware/top-value-scores")
    suspend fun getTopValueScores(): TopValueScoresResponse
}

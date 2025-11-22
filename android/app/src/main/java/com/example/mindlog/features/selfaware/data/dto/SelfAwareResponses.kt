package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName
import java.util.Dictionary


data class QuestionResponse(
    @SerializedName("id")             val id: Int,
    @SerializedName("question_type")  val questionType: String,
    @SerializedName("text")           val text: String,
    @SerializedName("created_at")     val createdAt: String // ISO8601
)

data class AnswerResponse(
    @SerializedName("id")          val id: Int,
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("type")        val type: String,
    @SerializedName("text")        val text: String,
    @SerializedName("created_at")  val createdAt: String, // ISO8601
    @SerializedName("updated_at")  val updatedAt: String // ISO8601
)

data class QAResponse(
    @SerializedName("question") val question: QuestionResponse,
    @SerializedName("answer") val answer: AnswerResponse? = null
)

data class QACursorResponse(
    @SerializedName("items") val items: List<QAResponse>,
    @SerializedName("next_cursor") val next_cursor: Int
)

data class ValueScoreResponse(
    @SerializedName("value")     val value: String,
    @SerializedName("intensity") val intensity: Float
)

data class TopValueScoresResponse(
    @SerializedName("value_scores") val valueScores: List<ValueScoreResponse>,
)

data class CategoryResponse(
    @SerializedName("category_en") val categoryEn: String,
    @SerializedName("category_ko") val categoryKo: String,
    @SerializedName("score") val score: Int
)

data class ValueMapResponse(
    @SerializedName("category_scores") val categoryScores: List<CategoryResponse>,
    @SerializedName("updated_at") val updatedAt: String // ISO8601
)

package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName

data class AnswerRequest(
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("text") val text: String
)
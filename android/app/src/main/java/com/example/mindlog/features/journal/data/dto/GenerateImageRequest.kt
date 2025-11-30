package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

/**
 * AI 이미지 생성을 요청할 때 서버로 보내는 데이터 모델
 */
data class GenerateImageRequest(
    @SerializedName("style")
    val style: String,

    @SerializedName("content")
    val content: String
)

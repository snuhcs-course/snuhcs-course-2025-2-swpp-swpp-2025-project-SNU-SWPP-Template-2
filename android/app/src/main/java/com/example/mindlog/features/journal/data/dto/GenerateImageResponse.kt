package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

/**
 * AI 이미지 생성 후 서버로부터 받는 데이터 모델
 */
data class GenerateImageResponse(
    @SerializedName("image_base64")
    val imageBase64: String
)

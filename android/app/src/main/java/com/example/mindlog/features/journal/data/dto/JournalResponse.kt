package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 서버로부터 받은 생성된 일기 데이터 모델 (Response Body)
 * API 문서의 응답 형식에 맞춰 필요한 필드들을 정의합니다.
 */
data class JournalResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    // API 응답에 포함된 emotions 필드를 추가합니다.
    @SerializedName("emotions")
    val emotions: List<EmotionResponse>,

    // API 응답에 포함된 keywords, image_s3_keys 등 다른 필드들도 필요시 여기에 추가할 수 있습니다.

    @SerializedName("created_at")
    val createdAt: Date // Gson 라이브러리가 ISO 8601 형식의 문자열을 Date 객체로 자동 변환해줍니다.
)

/**
 * JournalResponse 내부에 포함된 개별 감정 데이터 모델
 */
data class EmotionResponse(
    @SerializedName("emotion")
    val emotion: String,

    @SerializedName("intensity")
    val intensity: Int
)

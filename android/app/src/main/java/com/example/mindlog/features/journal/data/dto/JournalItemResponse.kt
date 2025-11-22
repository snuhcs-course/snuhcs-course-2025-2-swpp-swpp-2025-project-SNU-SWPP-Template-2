package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

/**
 * JournalListResponse 및 GetJournalById API의 응답 본문을 나타내는 데이터 클래스
 */
data class JournalItemResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("emotions")
    val emotions: List<EmotionResponse>,

    @SerializedName("gratitude")
    val gratitude: String,

    @SerializedName("image_s3_keys")
    val imageS3Keys: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("keywords")
    val keywords: List<KeywordResponse>?
)

package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

/**
 * `POST /journal/{journal_id}/analyze` API의 응답을 나타내는 최상위 데이터 클래스
 */
data class KeywordListResponse(
    @SerializedName("data")
    val data: List<KeywordResponse>
)

/**
 * 개별 키워드 정보를 담는 데이터 클래스
 */
data class KeywordResponse(
    @SerializedName("keyword")
    val keyword: String,

    @SerializedName("emotion")
    val emotion: String,

    @SerializedName("summary")
    val summary: String,

    @SerializedName("weight")
    val weight: Float
)

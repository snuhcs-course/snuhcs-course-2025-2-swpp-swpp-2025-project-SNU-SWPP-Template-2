package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

/**
 * GET /journal/me API의 응답을 나타내는 최상위 데이터 클래스
 */
data class JournalListResponse(
    @SerializedName("items")
    val items: List<JournalItemResponse>,

    @SerializedName("next_cursor")
    val nextCursor: Int? // 다음 페이지가 없으면 null일 수 있음
)

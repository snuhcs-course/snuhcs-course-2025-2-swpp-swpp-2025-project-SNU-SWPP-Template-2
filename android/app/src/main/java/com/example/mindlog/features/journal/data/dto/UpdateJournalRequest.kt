package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

/**
 * 일기 수정을 서버에 요청할 때 보내는 데이터 모델 (PATCH Request Body)
 * 모든 필드가 nullable이므로, 변경된 필드만 포함하여 요청할 수 있다.
 */
data class UpdateJournalRequest(
    @SerializedName("title")
    val title: String?,

    @SerializedName("content")
    val content: String?,

    @SerializedName("gratitude")
    val gratitude: String?
)

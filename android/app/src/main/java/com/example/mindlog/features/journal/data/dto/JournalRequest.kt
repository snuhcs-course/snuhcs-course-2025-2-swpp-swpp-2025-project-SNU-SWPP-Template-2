package com.example.mindlog.features.journal.data.dto // 패키지 경로는 실제 프로젝트에 맞게 확인해주세요.

import com.google.gson.annotations.SerializedName

/**
 * 일기 생성을 서버에 요청할 때 보내는 데이터 모델 (Request Body)
 *
 * @property title 일기 제목
 * @property content 일기 내용
 * @property emotions 감정 점수 맵. (예: {"happy": 3, "sad": 1})
 * @property gratitude 감사 일기 내용
 */
data class JournalRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("emotions")
    val emotions: Map<String, Int>,

    @SerializedName("gratitude")
    val gratitude: String
)

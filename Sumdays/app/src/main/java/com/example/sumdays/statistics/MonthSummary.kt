package com.example.sumdays.statistics

// 월간 요약 데이터를 담을 클래스 (포도 상징)
// 'ai/summarize-month' API의 result 객체 전체를 파싱
// MonthSummary.kt 파일

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ⭐ WeekSummaryForMonth에 Parcelable 적용
@Parcelize
data class WeekSummaryForMonth(
    val emotionScore: Float,
    val dominantEmoji: String,
    val topics: List<String>,
    val title: String,
    val summary: String,
    val dateRange: String
) : Parcelable // <-- Parcelable 상속

// ⭐ MonthSummary에 신규 필드 추가 및 Parcelable 적용
@Parcelize
data class MonthSummary(
    // ⭐ 새로 추가된 필드
    val startDate: String,      // 월의 시작일 (예: "2025-09-01")
    val endDate: String,        // 월의 종료일 (예: "2025-09-30")
    val diaryCount: Int,        // 월간 작성된 총 일기 횟수 (예: 25)

    // 기존 필드
    val insights: Insights,
    val summary: SummaryDetails,
    val weeksForMonth: List<WeekSummaryForMonth>,
    val emotionAnalysis: EmotionAnalysis
) : Parcelable // <-- Parcelable 상속
package com.example.sumdays.statistics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// 주간 요약 데이터를 담을 클래스 (초록 블록 상징)
// 'ai/summarize-week' API의 result 객체 전체를 파싱
@Parcelize
data class WeekSummary(
    val startDate: String,      // 예: "2025-10-13"
    val endDate: String,        // 예: "2025-10-19"
    val diaryCount: Int,        // 예: 5 (작성된 일기 수)
    // 1. 감정 분석 (emotion_analysis)
    val emotionAnalysis: EmotionAnalysis,

    // 2. 하이라이트 (highlights) - 주간 요약 텍스트로 사용될 수 있는 핵심 일기 목록
    val highlights: List<Highlight>,

    // 3. 통찰/조언 (insights)
    val insights: Insights,

    // 4. 요약 개요 (summary)
    val summary: SummaryDetails
): Parcelable

// WeekSummary 내부 클래스들 정의
@Parcelize
data class EmotionAnalysis(
    val distribution: Map<String, Int>, // 감정 분포 (positive, neutral, negative)
    val dominantEmoji: String,
    val emotionScore: Float,
    val trend: String? = null // 감정 추이 (increasing, decreasing)
): Parcelable
@Parcelize
data class Highlight(
    val date: String,
    val summary: String // 핵심 요약 텍스트
): Parcelable

@Parcelize
data class Insights(
    val advice: String,
    val emotionCycle: String
): Parcelable

@Parcelize
data class SummaryDetails(
    val emergingTopics: List<String>, // 주요 키워드
    val overview: String,            // 주간 개요 설명
    val title: String                // 주간 요약 제목
): Parcelable
// src/test/java/com/example/sumdays/statistics/TestParcelableData.kt

package com.example.sumdays.statistics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// --- 공통으로 사용되는 Parcelable 데이터 클래스 정의 ---

@Parcelize
data class SummaryDetails(
    val emergingTopics: List<String>,
    val overview: String,
    val title: String
) : Parcelable

@Parcelize
data class EmotionAnalysis(
    val distribution: Map<String, Int>,
    val dominantEmoji: String,
    val emotionScore: Double,
    val trend: String? = null
) : Parcelable

@Parcelize
data class Insights(
    val advice: String,
    val emotionCycle: String
) : Parcelable

@Parcelize
data class Highlight(
    val date: String,
    val summary: String
) : Parcelable
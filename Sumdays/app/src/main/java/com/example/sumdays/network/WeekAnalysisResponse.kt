package com.example.sumdays.network

import com.example.sumdays.data.EmotionAnalysis
import com.example.sumdays.data.Highlight
import com.example.sumdays.data.Insights
import com.example.sumdays.data.SummaryDetails
import com.example.sumdays.data.WeekSummary
import com.google.gson.annotations.SerializedName

data class WeekAnalysisResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("result")
    val result: WeekAnalysisResult?
)

// API의 "result" 객체에 대응
data class WeekAnalysisResult(
    @SerializedName("emotion_analysis")
    val emotionAnalysis: NetworkEmotionAnalysis,

    @SerializedName("highlights")
    val highlights: List<NetworkHighlight>,

    @SerializedName("insights")
    val insights: NetworkInsights,

    @SerializedName("summary")
    val summary: NetworkSummaryDetails
){
    fun toWeekSummary(startDate: String, endDate: String, diaryCount: Int): WeekSummary {
        return WeekSummary(
            startDate = startDate,
            endDate = endDate,
            diaryCount = diaryCount,
            emotionAnalysis = this.emotionAnalysis.toDomain(),
            highlights = this.highlights.map { it.toDomain() },
            insights = this.insights.toDomain(),
            summary = this.summary.toDomain()
        )
    }
}

// 하위 DTO 클래스들

data class NetworkEmotionAnalysis(
    @SerializedName("distribution")
    val distribution: Map<String, Int>, // {"positive": 4, ...}

    @SerializedName("dominant_emoji")
    val dominantEmoji: String,

    @SerializedName("emotion_score")
    val emotionScore: Double,

    @SerializedName("trend")
    val trend: String? // "decreasing"
){
    fun toDomain() = EmotionAnalysis(distribution, dominantEmoji, emotionScore, trend)
}

data class NetworkHighlight(
    @SerializedName("date")
    val date: String,

    @SerializedName("summary")
    val summary: String
) {
    fun toDomain() = Highlight(date, summary)
}

data class NetworkInsights(
    @SerializedName("advice")
    val advice: String,

    @SerializedName("emotion_cycle")
    val emotionCycle: String
) {
    fun toDomain() = Insights(advice, emotionCycle)
}

data class NetworkSummaryDetails(
    @SerializedName("emerging_topics")
    val emergingTopics: List<String>,

    @SerializedName("overview")
    val overview: String,

    @SerializedName("title")
    val title: String
) {
    fun toDomain() = SummaryDetails(emergingTopics, overview, title)
}
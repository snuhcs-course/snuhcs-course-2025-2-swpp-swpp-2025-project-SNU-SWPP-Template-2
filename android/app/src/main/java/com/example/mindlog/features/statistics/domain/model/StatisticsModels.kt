package com.example.mindlog.features.statistics.domain.model

enum class Emotion(val apiValue: String) {
    HAPPY("happy"),
    SAD("sad"),
    ANXIOUS("anxious"),
    CALM("calm"),
    ANNOYED("annoyed"),
    SATISFIED("satisfied"),
    BORED("bored"),
    INTERESTED("interested"),
    LETHARGIC("lethargic"),
    ENERGETIC("energetic");

    companion object {
        fun fromApi(value: String): Emotion? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

data class EmotionRate(
    val emotion: Emotion,
    val count: Int,
    val percentage: Float
)

data class JournalStatistics(
    val EmotionTrends: List<EmotionTrend>,
    val EmotionEvents: List<EmotionEvent>,
    val JournalKeywords: List<JournalKeyword>
)

data class EmotionTrend(
    val emotion: Emotion,
    val trend: List<Int>
)

data class EmotionEvent(
    val emotion: Emotion,
    val events: List<String>
)

data class JournalKeyword(
    val keyword: String,
    val count: Int
)
package com.example.mindlog.statistics

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.statistics.domain.model.Emotion
import com.example.mindlog.features.statistics.domain.model.EmotionEvent
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import javax.inject.Inject

class TestStatisticsRepository @Inject constructor() : StatisticsRepository {
    override suspend fun getEmotionRates(startDate: String, endDate: String): Result<List<EmotionRate>> {
        // Fixed, deterministic fake data for UI tests
        val rates = listOf(
            EmotionRate(Emotion.HAPPY, count = 20, percentage = 0.20f),
            EmotionRate(Emotion.SAD, count = 10, percentage = 0.10f),
            EmotionRate(Emotion.CALM, count = 5,  percentage = 0.05f),
            EmotionRate(Emotion.ANXIOUS, count = 8, percentage = 0.08f),
            EmotionRate(Emotion.ENERGETIC, count = 12, percentage = 0.12f),
            EmotionRate(Emotion.SATISFIED, count = 15, percentage = 0.15f),
            EmotionRate(Emotion.ANNOYED, count = 6, percentage = 0.06f),
            EmotionRate(Emotion.BORED, count = 4, percentage = 0.04f),
            EmotionRate(Emotion.INTERESTED, count = 9, percentage = 0.09f),
            EmotionRate(Emotion.LETHARGIC, count = 11, percentage = 0.11f)
        )
        return Result.Success(rates)
    }

    override suspend fun getJournalStatisics(startDate: String, endDate: String): Result<JournalStatistics> {
        // Trends are averaged-per-day series; keep lengths small for quick UI rendering
        val trends = listOf(
            EmotionTrend(Emotion.CALM, listOf(1, 2, 3, 2, 4)),
            EmotionTrend(Emotion.HAPPY, listOf(3, 4, 5, 4, 5)),
            EmotionTrend(Emotion.SAD,   listOf(2, 2, 1, 2, 1)),
        )

        // Events built as short lists per emotion
        val events = listOf(
            EmotionEvent(Emotion.HAPPY, listOf("기쁜 일", "칭찬 받음")),
            EmotionEvent(Emotion.SAD, listOf("실수함")),
            EmotionEvent(Emotion.ENERGETIC, listOf("퇴근 후 운동", "아침 조깅"))
        )

        // Top keywords by simple counts
        val keywords = listOf(
            JournalKeyword("운동", 3),
            JournalKeyword("퇴근", 2),
            JournalKeyword("독서", 1)
        )

        return Result.Success(
            JournalStatistics(
                EmotionTrends = trends,
                EmotionEvents = events,
                JournalKeywords = keywords
            )
        )
    }
}
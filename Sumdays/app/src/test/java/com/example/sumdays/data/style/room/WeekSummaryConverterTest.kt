package com.example.sumdays.data.converter

import com.example.sumdays.statistics.*
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekSummaryConverterTest {

    private val converter = WeekSummaryConverter()

    @Test
    fun fromWeekSummary_and_toWeekSummary_should_serialize_and_deserialize_correctly() {

        // 1. 테스트용 dummy WeekSummary 만들기
        val summary = WeekSummary(
            startDate = "2025-01-01",
            endDate = "2025-01-07",
            diaryCount = 5,
            emotionAnalysis = EmotionAnalysis(
                distribution = mapOf("positive" to 3, "neutral" to 1, "negative" to 1),
                dominantEmoji = "😊",
                emotionScore = 0.82f,
                trend = "increasing"
            ),
            highlights = listOf(
                Highlight("2025-01-02", "좋은 하루였다."),
                Highlight("2025-01-05", "운동을 했다.")
            ),
            insights = Insights(
                advice = "긍정적인 습관을 유지하세요.",
                emotionCycle = "upward"
            ),
            summary = SummaryDetails(
                emergingTopics = listOf("운동", "긍정"),
                overview = "전반적으로 안정적이고 긍정적입니다.",
                title = "좋은 한 주"
            )
        )

        // 2. WeekSummary → String 변환
        val json = converter.fromWeekSummary(summary)

        // 3. 다시 String → WeekSummary 변환
        val restored = converter.toWeekSummary(json)

        // 4. 모든 필드가 동일해야 한다
        assertEquals(summary.startDate, restored.startDate)
        assertEquals(summary.endDate, restored.endDate)
        assertEquals(summary.diaryCount, restored.diaryCount)

        // emotionAnalysis
        assertEquals(summary.emotionAnalysis.dominantEmoji, restored.emotionAnalysis.dominantEmoji)
        assertEquals(summary.emotionAnalysis.emotionScore, restored.emotionAnalysis.emotionScore)
        assertEquals(summary.emotionAnalysis.trend, restored.emotionAnalysis.trend)
        assertEquals(summary.emotionAnalysis.distribution, restored.emotionAnalysis.distribution)

        // highlights
        assertEquals(summary.highlights.size, restored.highlights.size)
        assertEquals(summary.highlights[0].date, restored.highlights[0].date)
        assertEquals(summary.highlights[0].summary, restored.highlights[0].summary)

        // insights
        assertEquals(summary.insights.advice, restored.insights.advice)
        assertEquals(summary.insights.emotionCycle, restored.insights.emotionCycle)

        // summary
        assertEquals(summary.summary.title, restored.summary.title)
        assertEquals(summary.summary.overview, restored.summary.overview)
        assertEquals(summary.summary.emergingTopics, restored.summary.emergingTopics)
    }
}

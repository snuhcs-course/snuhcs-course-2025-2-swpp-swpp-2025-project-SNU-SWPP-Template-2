package com.example.mindlog.features.statistics.data.mapper

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.statistics.data.dto.EmotionRateItem
import com.example.mindlog.features.statistics.data.dto.EmotionRatesResponse
import com.example.mindlog.features.statistics.domain.model.Emotion
import com.example.mindlog.features.statistics.domain.model.EmotionEvent
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import javax.inject.Inject

class StatisticsMapper @Inject constructor() {
    private fun parseEmotion(value: String?): Emotion =
        value?.let { Emotion.fromApi(it) } ?: Emotion.CALM

    fun toEmotionRate(dto: EmotionRateItem) = EmotionRate(
        emotion = Emotion.fromApi(dto.emotion) ?: Emotion.CALM,
        count = dto.count,
        percentage = if (dto.percentage > 1f) dto.percentage / 100f else dto.percentage
    )

    fun toJournalStatistics(journals: List<JournalItemResponse>): JournalStatistics {
        // 감정 트렌드 (날짜별 감정 intensity 평균)
        val trendByEmotion = mutableMapOf<Emotion, MutableList<Pair<String, Int>>>()
        for (journal in journals) {
            val date = journal.createdAt.take(10) // "2025-11-09T..." → "2025-11-09"
            for (emotion in journal.emotions) {
                val emo = Emotion.fromApi(emotion.emotion) ?: continue
                trendByEmotion
                    .getOrPut(emo) { mutableListOf() }
                    .add(date to emotion.intensity)
            }
        }

        val emotionTrends = trendByEmotion.map { (emotion: Emotion, entries) ->
            val grouped = entries.groupBy { it.first }
            val dailyAvg = grouped.mapValues { (_, list) ->
                list.map { it.second }.average().toInt()
            }
            // 날짜 순으로 intensity 정렬
            EmotionTrend(
                emotion = emotion,
                trend = dailyAvg.toSortedMap().values.toList()
            )
        }

        // 감정 이벤트 (강한 intensity를 가진 일기의 제목 or 요약)
        val emotionEvents = journals.flatMap { journal ->
            (journal.keywords ?: emptyList())
                .asSequence()
                .filter { it.weight >= 0.7f } // 키워드 신뢰도 임계값
                .mapNotNull { kw ->
                    val emo = Emotion.fromApi(kw.emotion) ?: return@mapNotNull null
                    val text = (kw.summary?.takeIf { it.isNotBlank() } ?: kw.keyword).trim()
                    if (text.isBlank()) null else emo to text
                }
                .toList()
        }
            .groupBy({ it.first }, { it.second })
            .map { (emotion, texts) ->
                // 같은/유사 텍스트 중복 제거 후 최대 5개까지만 사용
                val distinctTexts = texts.distinct().take(5)
                EmotionEvent(emotion = emotion, events = distinctTexts)
            }

        // 키워드 빈도 (모든 일기의 keywords)
        val keywordCount = mutableMapOf<String, Int>()
        for (journal in journals) {
            journal.keywords?.forEach {
                keywordCount[it.keyword] = (keywordCount[it.keyword] ?: 0) + 1
            }
        }

        val journalKeywords = keywordCount.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { (k, v) -> JournalKeyword(keyword = k, count = v) }

        // 최종 조합
        return JournalStatistics(
            EmotionTrends = emotionTrends,
            EmotionEvents = emotionEvents,
            JournalKeywords = journalKeywords
        )
    }
}
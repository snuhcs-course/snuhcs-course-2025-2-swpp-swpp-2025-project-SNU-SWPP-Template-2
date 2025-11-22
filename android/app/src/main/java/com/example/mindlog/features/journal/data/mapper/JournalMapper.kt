package com.example.mindlog.features.journal.data.mapper

import com.example.mindlog.BuildConfig
import com.example.mindlog.core.model.Emotion
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.features.journal.data.dto.EmotionResponse
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.KeywordResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Journal 관련 DTO를 Domain/UI 모델로 변환하는 매퍼 클래스.
 */
class JournalMapper @Inject constructor() {

    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    /**
     * JournalItemResponse(DTO)를 JournalEntry(UI Model)로 변환합니다.
     */
    fun toJournalEntry(dto: JournalItemResponse): JournalEntry {
        val imageUrl = dto.imageS3Keys?.let { s3Key ->
            if (s3Key.isNotBlank()) "${BuildConfig.S3_BUCKET_URL}/$s3Key" else null
        }

        val createdAtDate = try {
            serverDateFormat.parse(dto.createdAt)!!
        } catch (e: Exception) {
            Date()
        }

        val keywordsList = dto.keywords?.map { keywordDto ->
            Keyword(
                keyword = keywordDto.keyword,
                emotion = keywordDto.emotion,
                summary = keywordDto.summary,
                weight = keywordDto.weight
            )
        } ?: emptyList()

        val emotionsList = dto.emotions.map { toEmotion(it) }

        return JournalEntry(
            id = dto.id,
            title = dto.title,
            content = dto.content,
            createdAt = createdAtDate,
            imageUrl = imageUrl,
            keywords = keywordsList,
            emotions = emotionsList,
            gratitude = dto.gratitude
        )
    }

    /**
     * EmotionResponse(DTO)를 Emotion(UI Model)으로 변환합니다.
     */
    private fun toEmotion(dto: EmotionResponse): Emotion {
        return Emotion(
            emotion = dto.emotion,
            intensity = dto.intensity
        )
    }

    /**
     * KeywordResponse(DTO)를 Keyword(UI Model)로 변환합니다.
     */
    fun toKeyword(dto: KeywordResponse): Keyword {
        return Keyword(
            keyword = dto.keyword,
            emotion = dto.emotion,
            summary = dto.summary,
            weight = dto.weight
        )
    }
}

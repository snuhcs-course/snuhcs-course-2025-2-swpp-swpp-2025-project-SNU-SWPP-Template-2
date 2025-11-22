package com.example.mindlog.features.selfaware.data.mapper

import com.example.mindlog.features.selfaware.data.dto.*
import com.example.mindlog.features.selfaware.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// feature/selfaware/data/mapper/SelfAwareMapper.kt
class SelfAwareMapper @Inject constructor() {

    fun parseToLocalDate(raw: String?): LocalDate {
        if (raw.isNullOrBlank()) return LocalDate.now()

        return try {
            // 1) offset 포함된 ISO 문자열: 2025-11-16T21:41:42.286385+09:00
            OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()
        } catch (_: Exception) {
            try {
                // 2) offset 없는 ISO_LOCAL_DATE_TIME: 2025-11-16T21:42:07
                LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
            } catch (_: Exception) {
                try {
                    // 3) 혹시 그냥 날짜만 오는 경우
                    LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) {
                    // 혹시 이상한 값이 와도 앱이 안죽도록 fallback
                    LocalDate.now()
                }
            }
        }
    }

    fun toQuestion(dto: QuestionResponse) = Question(
        id = dto.id,
        type = dto.questionType,
        text = dto.text,
        createdAt = parseToLocalDate(dto.createdAt)
    )

    fun toAnswer(dto: AnswerResponse) = Answer(
        id = dto.id,
        questionId = dto.questionId,
        type = dto.type,
        text = dto.text,
        createdAt = parseToLocalDate(dto.createdAt),
        updatedAt = parseToLocalDate(dto.updatedAt),
    )

    fun toQAItem(dto: QAResponse) = QAItem(
        question = toQuestion(dto.question),
        answer = dto.answer?.let(::toAnswer)
    )

    fun toValueScore(dto: ValueScoreResponse): ValueScore {
        return ValueScore(
            value = dto.value,
            intensity = dto.intensity
        )
    }

    fun toTopValueScores(dto: TopValueScoresResponse): TopValueScores {
        return TopValueScores(
            valueScores = dto.valueScores.map(::toValueScore)
        )
    }

    fun toCategory(dto: CategoryResponse): CategoryScore {
        return CategoryScore(
            categoryEn = dto.categoryEn,
            categoryKo = dto.categoryKo,
            score = dto.score
        )
    }

    fun toValueMap(dto: ValueMapResponse): ValueMap {
        return ValueMap(
            categoryScores = dto.categoryScores.map(::toCategory),
            updatedAt = parseToLocalDate(dto.updatedAt)
        )
    }
}
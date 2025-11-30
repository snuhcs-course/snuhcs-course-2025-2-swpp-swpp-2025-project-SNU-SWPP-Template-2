package com.example.mindlog.features.selfaware.data.mapper

import com.example.mindlog.features.selfaware.data.dto.*
import com.example.mindlog.features.selfaware.domain.model.*
import java.time.LocalDateTime
import javax.inject.Inject

// feature/selfaware/data/mapper/SelfAwareMapper.kt
class SelfAwareMapper @Inject constructor() {

    fun parseLocalDateTime(s: String) = LocalDateTime.parse(s).toLocalDate()

    fun toQuestion(dto: QuestionResponse) = Question(
        id = dto.id,
        type = dto.questionType,
        text = dto.text,
        createdAt = parseLocalDateTime(dto.createdAt)
    )

    fun toAnswer(dto: AnswerResponse) = Answer(
        id = dto.id,
        questionId = dto.questionId,
        type = dto.type,
        text = dto.text,
        createdAt = parseLocalDateTime(dto.createdAt),
        updatedAt = parseLocalDateTime(dto.updatedAt),
    )

    fun toQAItem(dto: QAResponse) = QAItem(
        question = toQuestion(dto.question),
        answer = dto.answer?.let(::toAnswer)
    )

    private fun toValueScore(dto: ValueScoreResponse): ValueScore {
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


    private fun toCategory(dto: CategoryResponse): CategoryScore {
        return CategoryScore(
            categoryEn = dto.categoryEn,
            categoryKo = dto.categoryKo,
            score = dto.score
        )
    }

    fun toValueMap(dto: ValueMapResponse): ValueMap {
        return ValueMap(
            categoryScores = dto.categoryScores.map(::toCategory),
            updatedAt = parseLocalDateTime(dto.updatedAt)
        )
    }
}
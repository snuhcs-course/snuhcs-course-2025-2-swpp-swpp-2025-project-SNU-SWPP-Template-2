package com.example.mindlog.features.selfaware.domain.model

import java.time.LocalDate

data class Question(
    val id: Int,
    val type: String,
    val text: String,
    val createdAt: LocalDate
)

data class Answer(
    val id: Int,
    val questionId: Int,
    val type: String?,
    val text: String,
    val createdAt: LocalDate,
    val updatedAt: LocalDate,
)

data class QAItem(
    val question: Question,
    val answer: Answer?
)


data class ValueScore(
    val value: String,
    val intensity: Float
)

data class TopValueScores(
    val valueScores: List<ValueScore>
)

data class CategoryScore(
    val categoryEn: String,
    val categoryKo: String,
    val score: Int
)

data class ValueMap(
    val categoryScores: List<CategoryScore>,
    val updatedAt: LocalDate
)

data class PersonalityInsight(
    val comment: String,
    val personalityInsight: String,
    val updatedAt: LocalDate
)



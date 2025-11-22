package com.example.mindlog.selfaware

import com.example.mindlog.features.selfaware.data.dto.AnswerResponse
import com.example.mindlog.features.selfaware.data.dto.CategoryResponse
import com.example.mindlog.features.selfaware.data.dto.QAResponse
import com.example.mindlog.features.selfaware.data.dto.QuestionResponse
import com.example.mindlog.features.selfaware.data.dto.TopValueScoresResponse
import com.example.mindlog.features.selfaware.data.dto.ValueMapResponse
import com.example.mindlog.features.selfaware.data.dto.ValueScoreResponse
import com.example.mindlog.features.selfaware.data.mapper.SelfAwareMapper
import com.example.mindlog.features.selfaware.domain.model.Question
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelfAwareMapperTest {
    private val mapper = SelfAwareMapper()

    @Test
    fun `parseLocalDateTime correctly parses ISO string to LocalDate`() {
        val dateStr = "2025-03-18T10:30:00"
        val result = mapper.parseToLocalDate(dateStr)
        assertEquals(LocalDate.of(2025, 3, 18), result)
    }

    @Test
    fun `toQuestion maps fields correctly`() {
        val dto = QuestionResponse(
            id = 1,
            questionType = "single",
            text = "당신에게 의미 있었던 일은?",
            createdAt = "2025-03-18T10:30:00"
        )

        val question = mapper.toQuestion(dto)
        assertEquals(1, question.id)
        assertEquals("single", question.type)
        assertEquals("당신에게 의미 있었던 일은?", question.text)
        assertEquals(LocalDate.of(2025, 3, 18), question.createdAt)
    }

    @Test
    fun `toAnswer maps fields correctly`() {
        val dto = AnswerResponse(
            id = 2,
            questionId = 1,
            type = "text",
            text = "가족과 함께 저녁을 먹었다",
            createdAt = "2025-03-17T20:00:00",
            updatedAt = "2025-03-17T22:00:00"
        )

        val answer = mapper.toAnswer(dto)
        assertEquals(2, answer.id)
        assertEquals(1, answer.questionId)
        assertEquals("text", answer.type)
        assertEquals("가족과 함께 저녁을 먹었다", answer.text)
        assertEquals(LocalDate.of(2025, 3, 17), answer.createdAt)
        assertEquals(LocalDate.of(2025, 3, 17), answer.updatedAt)
    }

    @Test
    fun `toQAItem maps nested question and answer`() {
        val questionDto = QuestionResponse(
            id = 1,
            questionType = "multi",
            text = "하루 중 감사했던 일은?",
            createdAt = "2025-03-18T09:00:00"
        )
        val answerDto = AnswerResponse(
            id = 10,
            questionId = 1,
            type = "text",
            text = "친구와 진심 어린 대화를 나눴다",
            createdAt = "2025-03-18T20:00:00",
            updatedAt = "2025-03-18T20:30:00"
        )

        val qa = mapper.toQAItem(QAResponse(question = questionDto, answer = answerDto))

        assertEquals(1, qa.question.id)
        assertEquals("multi", qa.question.type)
        assertEquals("친구와 진심 어린 대화를 나눴다", qa.answer?.text)
    }

    @Test
    fun `toTopValueScores maps list correctly`() {
        val dto = TopValueScoresResponse(
            valueScores = listOf(
                ValueScoreResponse("성장", 80f),
                ValueScoreResponse("자유", 65f),
            )
        )

        val domain = mapper.toTopValueScores(dto)
        assertEquals(2, domain.valueScores.size)
        assertEquals("성장", domain.valueScores[0].value)
        assertEquals(80f, domain.valueScores[0].intensity)
    }

    @Test
    fun `toValueMap maps categories and updatedAt`() {
        val dto = ValueMapResponse(
            categoryScores = listOf(
                CategoryResponse("Growth", "성장", 80),
                CategoryResponse("RelationgShip", "관계", 60),
                CategoryResponse("Safe","안정", 50),
                CategoryResponse("Free","자유", 70),
                CategoryResponse("Achievement","성취", 55),
                CategoryResponse("Exciting","재미", 65),
                CategoryResponse("Ethics", "윤리", 75),
            ),
            updatedAt = "2025-03-18T22:10:00"
        )

        val domain = mapper.toValueMap(dto)
        assertEquals(7, domain.categoryScores.size)
        assertEquals("성장", domain.categoryScores[0].categoryKo)
        assertEquals(80, domain.categoryScores[0].score)
        assertEquals(LocalDate.of(2025, 3, 18), domain.updatedAt)
    }

}
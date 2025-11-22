package com.example.mindlog.analysis

import com.example.mindlog.features.analysis.data.dto.ComprehensiveAnalysisResponse
import com.example.mindlog.features.analysis.data.dto.PersonalizedAdviceResponse
import com.example.mindlog.features.analysis.data.dto.UserTypeResponse
import com.example.mindlog.features.analysis.data.mapper.AnalysisMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalysisMapperTest {

    private val mapper = AnalysisMapper()

    @Test
    fun `toUserType maps all fields correctly`() {
        val dto = UserTypeResponse(
            userType = "Explorer",
            description = "새로운 경험을 추구하는 타입",
            updatedAt = "2025-03-18T10:00:00"
        )

        val domain = mapper.toUserType(dto)

        assertEquals("Explorer", domain.userType)
        assertEquals("새로운 경험을 추구하는 타입", domain.description)
        assertEquals("2025-03-18T10:00:00", domain.updatedAt)
    }

    @Test
    fun `toComprehensive maps all fields correctly`() {
        val dto = ComprehensiveAnalysisResponse(
            conscientiousness = "높음",
            neuroticism = "낮음",
            extraversion = "중간",
            openness = "높음",
            agreeableness = "높음",
            updatedAt = "2025-03-18T11:00:00"
        )

        val domain = mapper.toComprehensive(dto)

        assertEquals("높음", domain.conscientiousness)
        assertEquals("낮음", domain.neuroticism)
        assertEquals("중간", domain.extraversion)
        assertEquals("높음", domain.openness)
        assertEquals("높음", domain.agreeableness)
        assertEquals("2025-03-18T11:00:00", domain.updatedAt)
    }

    @Test
    fun `toAdvice maps all fields correctly`() {
        val dto = PersonalizedAdviceResponse(
            adviceType = "생활 루틴",
            personalizedAdvice = "규칙적인 수면 패턴을 유지해보세요.",
            updatedAt = "2025-03-18T12:00:00"
        )

        val domain = mapper.toAdvice(dto)

        assertEquals("생활 루틴", domain.adviceType)
        assertEquals("규칙적인 수면 패턴을 유지해보세요.", domain.personalizedAdvice)
        assertEquals("2025-03-18T12:00:00", domain.updatedAt)
    }
}
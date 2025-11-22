package com.example.mindlog.features.analysis.data.mapper


import com.example.mindlog.features.analysis.data.dto.ComprehensiveAnalysisResponse
import com.example.mindlog.features.analysis.data.dto.PersonalizedAdviceResponse
import com.example.mindlog.features.analysis.data.dto.UserTypeResponse
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import javax.inject.Inject

class AnalysisMapper @Inject constructor() {
    fun toUserType(res: UserTypeResponse) = UserType(
        userType = res.userType,
        description = res.description,
        updatedAt = res.updatedAt
    )

    fun toComprehensive(res: ComprehensiveAnalysisResponse) = ComprehensiveAnalysis(
        conscientiousness = res.conscientiousness,
        neuroticism = res.neuroticism,
        extraversion = res.extraversion,
        openness = res.openness,
        agreeableness = res.agreeableness,
        updatedAt = res.updatedAt
    )

    fun toAdvice(res: PersonalizedAdviceResponse) = PersonalizedAdvice(
        adviceType = res.adviceType,
        personalizedAdvice = res.personalizedAdvice,
        updatedAt = res.updatedAt
    )
}
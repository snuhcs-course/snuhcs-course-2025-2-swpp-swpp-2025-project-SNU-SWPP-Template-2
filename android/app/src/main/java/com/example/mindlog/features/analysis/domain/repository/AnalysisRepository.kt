package com.example.mindlog.features.analysis.domain.repository

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType

interface AnalysisRepository {
    suspend fun getUserType(): Result<UserType>
    suspend fun getComprehensiveAnalysis(): Result<ComprehensiveAnalysis>
    suspend fun getPersonalizedAdvice(): Result<PersonalizedAdvice>
}
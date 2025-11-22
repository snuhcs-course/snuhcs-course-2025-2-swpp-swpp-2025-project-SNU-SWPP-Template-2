package com.example.mindlog.features.analysis.domain.usecase

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.analysis.domain.repository.AnalysisRepository
import javax.inject.Inject


class GetUserTypeUseCase @Inject constructor(private val repo: AnalysisRepository) {
    suspend operator fun invoke(): Result<UserType> = repo.getUserType()
}

class GetComprehensiveAnalysisUseCase @Inject constructor(private val repo: AnalysisRepository) {
    suspend operator fun invoke(): Result<ComprehensiveAnalysis> = repo.getComprehensiveAnalysis()
}

class GetPersonalizedAdviceUseCase @Inject constructor(private val repo: AnalysisRepository) {
    suspend operator fun invoke(): Result<PersonalizedAdvice> = repo.getPersonalizedAdvice()
}



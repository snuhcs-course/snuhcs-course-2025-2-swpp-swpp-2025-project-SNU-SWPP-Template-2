package com.example.mindlog.features.analysis.data.repository

import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.toResult
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.analysis.data.api.AnalysisApi
import com.example.mindlog.features.analysis.data.mapper.AnalysisMapper
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.analysis.domain.repository.AnalysisRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val api: AnalysisApi,
    private val mapper: AnalysisMapper,
    private val dispatcher: DispatcherProvider
) : AnalysisRepository {

    override suspend fun getUserType(): Result<UserType> = withContext(dispatcher.io) {
        runCatching {
            val res = api.getUserType()
            mapper.toUserType(res)
        }.toResult()
    }

    override suspend fun getComprehensiveAnalysis(): Result<ComprehensiveAnalysis> = withContext(dispatcher.io) {
        runCatching {
            val res = api.getComprehensiveAnalysis()
            mapper.toComprehensive(res)
        }.toResult()
    }

    override suspend fun getPersonalizedAdvice(): Result<PersonalizedAdvice> = withContext(dispatcher.io) {
        runCatching {
            val res = api.getPersonalizedAdvice()
            mapper.toAdvice(res)
        }.toResult()
    }
}
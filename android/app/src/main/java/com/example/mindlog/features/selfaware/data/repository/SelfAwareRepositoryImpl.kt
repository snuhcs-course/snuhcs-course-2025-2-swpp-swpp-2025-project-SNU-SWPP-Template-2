package com.example.mindlog.features.selfaware.data.repository

import com.example.mindlog.core.common.toResult
import com.example.mindlog.core.common.Paged
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.data.api.SelfAwareApi
import com.example.mindlog.features.selfaware.data.dto.AnswerRequest
import com.example.mindlog.features.selfaware.data.mapper.SelfAwareMapper
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class SelfAwareRepositoryImpl @Inject constructor(
    private val api: SelfAwareApi,
    private val mapper: SelfAwareMapper,
    private val dispatcher: DispatcherProvider
) : SelfAwareRepository {

    override suspend fun getTodayQA(date: LocalDate) = withContext(dispatcher.io) {
        runCatching { mapper.toQAItem(api.getTodayQA(date.toString())) }.toResult()
    }

    override suspend fun submitAnswer(questionId: Int, answer: String) = withContext(dispatcher.io) {
        runCatching {
            val res = api.submitAnswer(AnswerRequest(questionId, answer))
            mapper.toAnswer(res)
        }.toResult()
    }

    override suspend fun getQAHistory(limit: Int, cursor: Int?) = withContext(dispatcher.io) {
        runCatching {
            val dto = api.getQAHistory(limit = limit, cursor = cursor)
            Paged(
                items = dto.items.map(mapper::toQAItem),
                cursor = dto.next_cursor,
                size = limit
            )
        }.toResult()
    }

    override suspend fun getTopValueScores() = withContext(dispatcher.io) {
        runCatching {
            val res = api.getTopValueScores()
            mapper.toTopValueScores(res)
        }.toResult()
    }

    override suspend fun getValueMap() = withContext(dispatcher.io) {
        runCatching {
            val res = api.getValueMap()
            mapper.toValueMap(res)
        }.toResult()
    }
}
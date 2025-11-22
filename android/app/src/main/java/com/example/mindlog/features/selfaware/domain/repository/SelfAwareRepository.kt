package com.example.mindlog.features.selfaware.domain.repository

import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.Paged
import com.example.mindlog.features.selfaware.domain.model.Answer
import com.example.mindlog.features.selfaware.domain.model.PersonalityInsight
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.model.TopValueScores
import com.example.mindlog.features.selfaware.domain.model.ValueMap
import java.time.LocalDate

interface SelfAwareRepository {
    suspend fun getTodayQA(date: LocalDate): Result<QAItem>
    suspend fun submitAnswer(questionId: Int, answer: String): Result<Answer>
    suspend fun getQAHistory(limit: Int, cursor: Int?): Result<Paged<QAItem>>

    suspend fun getTopValueScores(): Result<TopValueScores>
    suspend fun getValueMap(): Result<ValueMap>
}

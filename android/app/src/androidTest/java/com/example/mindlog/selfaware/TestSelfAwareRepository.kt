package com.example.mindlog.selfaware

import com.example.mindlog.core.common.Paged
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.selfaware.domain.model.*
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import javax.inject.Inject

class TestSelfAwareRepository @Inject constructor() : SelfAwareRepository {
    private val delayMs: Long = 0L    // 필요하면 살짝 지연도 가능
    private val withQuestion: Boolean = true // 폴링 없이 바로 질문을 줄지 여부
    private var getQAHistoryCallCount = 0;

    override suspend fun getTodayQA(date: LocalDate): Result<QAItem> {
        if (delayMs > 0) delay(delayMs)
        if (withQuestion) {
            val q = QAItem(
                question = Question(
                    id = 1,
                    type = "single_category",
                    text = "오늘 하루 가장 의미 있었던 순간은?",
                    createdAt = date
                ),
                answer = null
            )
            return Result.Success(q)
        } else return Result.Error(null, "time out")
    }

    override suspend fun submitAnswer(questionId: Int, answer: String): Result<Answer> {
        if (delayMs > 0) delay(delayMs)
        return Result.Success(
            Answer(
                id = 1,
                questionId = questionId,
                type = "text",
                text = answer,
                createdAt = LocalDate.now(),
                updatedAt = LocalDate.now()
            )
        )
    }

    override suspend fun getQAHistory(limit: Int, cursor: Int?): Result<Paged<QAItem>> {
        val now = LocalDate.now()
        return if (cursor == null) {
            // First call: return 10 QAItem objects
            getQAHistoryCallCount += 1
            Result.Success(
                Paged(
                    items = List(10) { idx ->
                        QAItem(
                            question = Question(
                                id = idx + 1,
                                type = "single",
                                text = "Q${idx + 1}: 질문 텍스트 예시",
                                createdAt = now
                            ),
                            answer = Answer(
                                id = 100 + idx,
                                questionId = idx + 1,
                                type = "text",
                                text = "A${idx + 1}: 답변 텍스트 예시",
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    },
                    cursor = 11,
                    size = 10
                )
            )
        } else {
            // Subsequent calls: return 5 QAItem objects
            Result.Success(
                Paged(
                    items = List(5) { idx ->
                        QAItem(
                            question = Question(
                                id = idx + cursor,
                                type = "single",
                                text = "Q${idx + cursor}: 추가 질문 예시",
                                createdAt = now
                            ),
                            answer = Answer(
                                id = idx + cursor + 100,
                                questionId = idx + cursor,
                                type = "text",
                                text = "A${idx + cursor + 100}: 추가 답변 예시",
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    },
                    cursor = null,
                    size = 5
                )
            )
        }
    }

    override suspend fun getTopValueScores(): Result<TopValueScores> {
        return Result.Success(
            TopValueScores(
                valueScores = listOf(
                    ValueScore("성장", 0.92f),
                    ValueScore("관계", 0.81f),
                    ValueScore("성취", 0.78f),
                    ValueScore("자유", 0.63f),
                    ValueScore("재미", 0.57f),
                )
            )
        )
    }

    override suspend fun getValueMap(): Result<ValueMap> {
        return Result.Success(
            ValueMap(
                categoryScores = listOf(
                    CategoryScore("Neuroticism", "불안정성", 80),
                    CategoryScore("Extroversion", "외향성", 60),
                    CategoryScore("Openness","개방성", 50),
                    CategoryScore("Agreeableness","수용성", 70),
                    CategoryScore("Conscientiousness","성실성", 55),
                ),
                updatedAt = LocalDate.now()
            )
        )
    }

}
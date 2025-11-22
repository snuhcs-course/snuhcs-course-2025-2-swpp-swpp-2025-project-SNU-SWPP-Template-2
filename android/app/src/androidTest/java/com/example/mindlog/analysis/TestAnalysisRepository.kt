package com.example.mindlog.analysis

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.analysis.domain.repository.AnalysisRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import javax.inject.Inject

class TestAnalysisRepository @Inject constructor() : AnalysisRepository {

    // 필요하면 테스트에서 살짝 지연을 줄 수 있도록
    private val delayMs: Long = 0L

    override suspend fun getUserType(): Result<UserType> {
        if (delayMs > 0) delay(delayMs)

        return Result.Success(
            UserType(
                userType = "탐험가형",
                description = "새로운 아이디어와 경험을 향해 끊임없이 움직이는 탐험가 타입이에요.",
                updatedAt = LocalDate.now().toString()
            )
        )
    }

    override suspend fun getComprehensiveAnalysis(): Result<ComprehensiveAnalysis> {
        if (delayMs > 0) delay(delayMs)

        return Result.Success(
            ComprehensiveAnalysis(
                conscientiousness = "계획을 세우고 지키는 편이며, 목표를 향해 꾸준히 움직이는 경향이 강합니다.",
                neuroticism = "정서적으로 비교적 안정적이지만, 중요한 순간에는 스스로를 많이 압박할 수 있어요.",
                extraversion = "필요할 때 사람들 속으로 들어가지만, 혼자 정리하는 시간도 중요하게 여깁니다.",
                openness = "새로운 아이디어와 실험적인 시도를 즐기는 편으로, 창의적인 분야에 잘 맞습니다.",
                agreeableness = "타인의 감정을 잘 살피고 조율하려는 편으로, 갈등보다는 조화를 선호합니다.",
                updatedAt = LocalDate.now().toString()
            )
        )
    }

    override suspend fun getPersonalizedAdvice(): Result<PersonalizedAdvice> {
        if (delayMs > 0) delay(delayMs)

        return Result.Success(
            PersonalizedAdvice(
                adviceType = "오늘의 탐험 가이드",
                personalizedAdvice = "오늘은 ‘완벽한 계획’보다 작은 시도를 먼저 해보는 게 좋아요.\n" +
                        "마음속에만 있던 아이디어 중 하나를 아주 가볍게 실험해보면 어떨까요?",
                updatedAt = LocalDate.now().toString()
            )
        )
    }
}
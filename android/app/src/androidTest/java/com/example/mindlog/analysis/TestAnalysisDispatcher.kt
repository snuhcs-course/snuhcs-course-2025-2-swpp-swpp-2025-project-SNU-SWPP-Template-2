package com.example.mindlog.analysis

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class TestAnalysisDispatcher : Dispatcher() {

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path ?: ""

        return when {
            // 1) 사용자 유형
            path.startsWith("/analysis/user-type") -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "user_type": "탐험가형",
                          "description": "새로운 아이디어와 경험을 찾아 스스로 경계를 넓혀가는 유형이에요.",
                          "updated_at": "2025-10-26T09:00:00"
                        }
                        """.trimIndent()
                    )
            }

            // 2) 종합 분석 (Five Factor)
            path.startsWith("/analysis/comprehensive-analysis") -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "conscientiousness": "목표를 세우고 계획적으로 움직이는 편이에요. 마감과 책임을 중요하게 생각합니다.",
                          "neuroticism": "일시적으로 불안해질 수 있지만, 전반적으로 감정을 잘 조절하는 편입니다.",
                          "extraversion": "혼자만의 시간을 좋아하지만, 의미 있는 사람들과의 깊은 대화에는 적극적으로 참여합니다.",
                          "openness": "새로운 아이디어와 실험을 즐기며, 지적 호기심이 강한 편이에요.",
                          "agreeableness": "타인의 감정을 세심하게 살피고 관계의 조화를 중요하게 여깁니다.",
                          "updated_at": "2025-10-26T09:10:00"
                        }
                        """.trimIndent()
                    )
            }

            // 3) 맞춤 조언
            path.startsWith("/analysis/personalized-advice") -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "advice_type": "오늘의 성장 포인트",
                          "personalized_advice": "오늘은 '완벽한 계획'보다 작은 시도를 먼저 해보는 날이에요. 머릿속에만 있던 아이디어 하나를 아주 가볍게 실행해보세요.",
                          "updated_at": "2025-10-26T09:15:00"
                        }
                        """.trimIndent()
                    )
            }

            else -> MockResponse().setResponseCode(404)
        }
    }
}
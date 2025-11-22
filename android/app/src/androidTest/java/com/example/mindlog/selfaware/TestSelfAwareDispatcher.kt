package com.example.mindlog.selfaware

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class TestSelfAwareDispatcher : Dispatcher() {

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path ?: ""

        return when {
            // 1) 오늘의 질문 (이미 답변한 경우 question+answer, 미답변이면 answer=null)
            path.startsWith("/self-aware/question") -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        // answer가 있는 경우
                        """
                        {
                          "question": {
                            "id": 13,
                            "question_type": "single_category",
                            "text": "가까운 사람과의 구체적인 한 에피소드를 떠올리며 그때 느낀 감정과 그것이 당신의 관계에 왜 중요한 의미로 남았는지 구체적으로 설명해 주세요?",
                            "categories_ko": ["관계와 연결"],
                            "categories_en": ["Relationships & Connection"],
                            "created_at": "2025-10-25T15:27:16"
                          },
                          "answer": {
                            "id": 6,
                            "question_id": 13,
                            "type": null,
                            "text": "얼마 전 친구와 사소한 오해로 말다툼을 했다...",
                            "created_at": "2025-10-25T16:16:12",
                            "updated_at": "2025-10-25T16:16:12"
                          }
                        }
                        """.trimIndent()
                    )
            }

            // 2) 답변 제출
            path.startsWith("/self-aware/answer") && request.method == "POST" -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "id": 100,
                          "question_id": 13,
                          "type": null,
                          "text": "테스트 답변 본문",
                          "created_at": "2025-10-26T10:00:00",
                          "updated_at": "2025-10-26T10:00:00"
                        }
                        """.trimIndent()
                    )
            }

            // 3) 가치 맵 (카테고리/점수)
            path.startsWith("/self-aware/value-map") -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "category_scores": [
                            {"category_en": "Neuroticism", "category_ko": "불안정성", "score": 72},
                            {"category_en": "Extroversion", "category_ko": "외향성", "score": 64},
                            {"category_en": "Openness", "category_ko": "개방성", "score": 58},
                            {"category_en": "Agreeableness", "category_ko": "수용성", "score": 61},
                            {"category_en": "Conscientiousness", "category_ko": "성실성", "score": 70}
                          ],
                          "updated_at": "2025-10-26T09:30:00"
                        }
                        """.trimIndent()
                    )
            }

            // 4) 상위 가치 키워드
            path.startsWith("/self-aware/top-value-scores") -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "value_scores": [
                            {"value": "성장", "intensity": 0.95},
                            {"value": "성취", "intensity": 0.90},
                            {"value": "자유", "intensity": 0.82},
                            {"value": "관계", "intensity": 0.80},
                            {"value": "안정", "intensity": 0.73}
                          ]
                        }
                        """.trimIndent()
                    )
            }


            // 6) 기록(페이징)
            path.startsWith("/self-aware/QA-history") -> {
                // 매우 단순화된 더미(첫 페이지 10개, next_cursor=2)
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "items": [
                            {
                              "question": {
                                "id": 1,
                                "question_type": "single_category",
                                "text": "Q1",
                                "created_at": "2025-10-20T10:00:00"
                              },
                              "answer": {
                                "id": 10,
                                "question_id": 1,
                                "type": null,
                                "text": "A1",
                                "created_at": "2025-10-20T10:10:00",
                                "updated_at": "2025-10-20T10:10:00"
                              }
                            }
                          ],
                          "next_cursor": 2
                        }
                        """.trimIndent()
                    )
            }

            else -> MockResponse().setResponseCode(404)
        }
    }
}
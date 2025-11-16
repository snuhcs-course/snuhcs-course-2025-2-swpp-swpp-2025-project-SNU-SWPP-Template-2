package com.example.sumdays.data.style

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_style")
data class UserStyle(
    @PrimaryKey(autoGenerate = true)
    val styleId: Long = 0, // PK: 각 스타일을 식별하는 고유 ID
    val styleName: String, // 사용자가 지정할 이름 (예: '시니컬 스타일', '경쾌한 스타일')
    val styleVector: List<Float>, // AI 스타일 벡터 (768차원)
    val styleExamples: List<String>, // 추출된 예시 문장들
    val stylePrompt: StylePrompt, // 상세 분석 결과 객체
    val sampleDiary: String = "" // 스타일 적용하여 생성한 샘플 일기
) {
    companion object {
        val Default = UserStyle(
            styleId = 0L,                // DB에 존재하지 않음을 의미
            styleName = "기본 스타일",
            styleVector = emptyList(),
            styleExamples = listOf(
                "자고 싶어! 졸려! 아무것도 하기 싫어~",
                "오늘은 그냥 아무 말도 하기 싫었다.",
                "일기는 왜 꼭 써야 하는 걸까?"
            ),
            stylePrompt = StylePrompt(
                tone = "일상적이고 솔직하며 감정 표현이 직접적",
                formality = "구어체",
                sentence_length = "짧음",
                sentence_structure = "단문 위주",
                sentence_endings = listOf("~", "~다", "~네"),
                lexical_choice = "편안하고 일상적인 단어 사용",
                common_phrases = listOf("그냥", "아무래도", "왜인지 모르게"),
                emotional_tone = "약간 무기력 + 솔직한 표현",
                irony_or_sarcasm = "거의 없음",
                slang_or_dialect = "반말",
                pacing = "보통"
            ),
            sampleDiary = "아침에 일어나서 좀 멍했어. 오늘은 카페에서 라떼 마시고 조용히 보낸 하루인 것 같아. 그런대로 지나가긴 했는데, 왜인지 모르게 기분이 묘하네. 그냥 일상적인 하루였지만, 별 의미 없이 흘러간 것 같아."
        )
    }

}
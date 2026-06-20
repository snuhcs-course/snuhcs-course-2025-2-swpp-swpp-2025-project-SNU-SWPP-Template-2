package com.example.sumdays.network

import com.example.sumdays.data.style.StylePrompt
import org.junit.Test
import kotlin.test.* // kotlin-test 라이브러리 사용

/**
 * StyleExtractionResponse 데이터 클래스의 유닛 테스트 (StylePrompt 정의 반영됨)
 */
class StyleExtractionResponseTest {

    /**
     * 테스트 1: 성공적인 응답 케이스
     * 'success = true' 및 모든 데이터가 정상적으로 할당되었는지 확인합니다.
     * (실제 StylePrompt 구조를 반영하여 수정됨)
     */
    @Test
    fun `성공_응답_케이스_데이터_정상_할당_확인`() {
        // Given
        val dummyPrompt = StylePrompt(
            character_concept = "일상적인 삶을 살아가는 평범한 사람. 소소한 일상을 관찰하고 기록하는 성향을 가진 인물.",
            emotional_tone = "감정이 드러나지 않고 중립적인 톤으로, 일상적인 사건을 기록하는 데 집중한다.",
            formality = "비격식적인 대화체로, 자연스러운 흐름을 유지하며 친근한 느낌을 준다.",
            lexical_choice = "일상적인 단어와 표현을 사용하여 친근함을 느끼게 한다.",
            pacing = "느긋하고 여유로운 흐름, 빠르지 않게 사건을 나열.",
            punctuation_style = "기본적인 문장 부호 사용, 복잡한 구두점은 없다.",
            sentence_endings = listOf("~었다.", "~했다.", "~었다고 생각했다."),
            sentence_length = "중간 길이의 문장들이 많으며, 간결하게 표현되어 있다.",
            sentence_structure = "주어-서술어 구조가 명확하며, 문장이 단순하고 직관적이다.",
            special_syntax = "일상적인 표현을 그대로 사용하며, 특별한 구문은 없음.",
            speech_quirks = "특별한 말투의 버릇은 없으며, 대화체적인 표현이 자연스럽다.",
            tone = "담담하고 차분한 어조로 일상의 소소한 사건들을 서술."
        )

        val dummyVector = listOf(0.1f, 0.2f, 0.3f)
        val dummyExamples = listOf("image_url_1.png", "image_url_2.png")

        // When
        val response = StyleExtractionResponse(
            success = true,
            style_vector = dummyVector,
            style_examples = dummyExamples,
            style_prompt = dummyPrompt,
            message = "Style extraction successful"
        )

        // Then
        assertTrue(response.success)
        assertEquals("Style extraction successful", response.message)

        // vector
        assertNotNull(response.style_vector)
        assertEquals(dummyVector, response.style_vector)

        // examples
        assertNotNull(response.style_examples)
        assertEquals(dummyExamples, response.style_examples)

        // StylePrompt 전체 검증
        val p = response.style_prompt!!
        assertEquals(dummyPrompt.character_concept, p.character_concept)
        assertEquals(dummyPrompt.emotional_tone,   p.emotional_tone)
        assertEquals(dummyPrompt.formality,        p.formality)
        assertEquals(dummyPrompt.lexical_choice,   p.lexical_choice)
        assertEquals(dummyPrompt.pacing,           p.pacing)
        assertEquals(dummyPrompt.punctuation_style, p.punctuation_style)
        assertEquals(dummyPrompt.sentence_endings, p.sentence_endings)
        assertEquals(dummyPrompt.sentence_length,  p.sentence_length)
        assertEquals(dummyPrompt.sentence_structure, p.sentence_structure)
        assertEquals(dummyPrompt.special_syntax,   p.special_syntax)
        assertEquals(dummyPrompt.speech_quirks,    p.speech_quirks)
        assertEquals(dummyPrompt.tone,             p.tone)
    }


    /**
     * 테스트 2: 실패 응답 케이스
     * 'success = false' 이고 데이터 필드들이 null이며, 에러 메시지가 포함된 경우를 확인합니다.
     * (이 테스트는 StylePrompt의 내부 구조와 무관하므로 수정할 필요가 없습니다.)
     */
    @Test
    fun `실패_응답_케이스_데이터_필드_null_확인`() {
        // When: 실패 응답 객체 생성 (데이터 필드 null)
        val response = StyleExtractionResponse(
            success = false,
            style_vector = null,
            style_examples = null,
            style_prompt = null,
            message = "Error: Invalid image"
        )

        // Then: 'success'는 false이고 데이터 필드들은 null인지 검증
        assertFalse(response.success)
        assertEquals("Error: Invalid image", response.message)

        assertNull(response.style_vector)
        assertNull(response.style_examples)
        assertNull(response.style_prompt)
    }

    /**
     * 테스트 3: 'success' 필드 기본값 확인 (중요)
     * 생성 시 'success' 파라미터를 명시적으로 전달하지 않았을 때,
     * 데이터 클래스에 정의된 기본값 'false'가 적용되는지 확인합니다.
     * (이 테스트도 StylePrompt의 내부 구조와 무관합니다.)
     */
    @Test
    fun `success_필드_미지정_시_기본값_false_확인`() {
        // When: 'success' 필드를 생략하고 객체 생성
        val response = StyleExtractionResponse(
            style_vector = null,
            style_examples = null,
            style_prompt = null,
            message = "Success field omitted"
        )

        // Then: 'success' 필드는 기본값인 false여야 함
        assertFalse(response.success)
        assertEquals("Success field omitted", response.message)
        assertNull(response.style_vector)
    }
}
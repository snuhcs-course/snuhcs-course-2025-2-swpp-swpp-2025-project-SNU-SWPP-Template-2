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
        // Given: 테스트에 사용할 더미 데이터 (수정된 StylePrompt 사용)
        val dummyPrompt = StylePrompt(
            common_phrases = listOf("like", "you know"),
            emotional_tone = "excited",
            formality = "informal",
            irony_or_sarcasm = "none",
            lexical_choice = "simple",
            pacing = "fast",
            sentence_endings = listOf("!", "."),
            sentence_length = "short",
            sentence_structure = "simple",
            slang_or_dialect = "none",
            tone = "enthusiastic"
        )
        val dummyVector = listOf(0.1f, 0.2f, 0.3f)
        val dummyExamples = listOf("image_url_1.png", "image_url_2.png")

        // When: 성공 응답 객체 생성
        val response = StyleExtractionResponse(
            success = true,
            style_vector = dummyVector,
            style_examples = dummyExamples,
            style_prompt = dummyPrompt,
            message = "Style extraction successful"
        )

        // Then: 모든 필드가 의도한 대로 설정되었는지 검증
        assertTrue(response.success)
        assertEquals("Style extraction successful", response.message)

        assertNotNull(response.style_vector)
        assertEquals(3, response.style_vector?.size)
        assertEquals(0.1f, response.style_vector?.get(0))

        assertNotNull(response.style_examples)
        assertEquals(2, response.style_examples?.size)
        assertEquals("image_url_1.png", response.style_examples?.get(0))

        // StylePrompt 검증 (수정된 부분)
        assertNotNull(response.style_prompt)
        assertEquals("excited", response.style_prompt?.emotional_tone)
        assertEquals("informal", response.style_prompt?.formality)
        assertEquals(listOf("like", "you know"), response.style_prompt?.common_phrases)
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
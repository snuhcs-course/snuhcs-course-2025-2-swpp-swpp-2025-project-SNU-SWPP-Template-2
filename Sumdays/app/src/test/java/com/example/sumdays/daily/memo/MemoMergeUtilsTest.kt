package com.example.sumdays.daily.memo

import com.example.sumdays.data.style.StylePrompt
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class MemoMergeUtilsTest {
    @Test
    fun convertStylePromptToMap_convertsAllFieldsCorrectly() {
        // given
        val prompt = StylePrompt(
            tone = "일상적이고 솔직한 톤",
            formality = "구어체",
            sentence_length = "짧음",
            sentence_structure = "단문 위주",
            sentence_endings = listOf("~다", "~네", "~야"),
            lexical_choice = "편안하고 일상적인 단어",
            common_phrases = listOf("그냥", "아무래도"),
            emotional_tone = "약간 무기력",
            irony_or_sarcasm = "거의 없음",
            slang_or_dialect = "반말",
            pacing = "보통"
        )

        // when
        val map = MemoMergeUtils.convertStylePromptToMap(prompt)

        // then
        // 기본 문자열 필드들
        assertEquals("일상적이고 솔직한 톤", map["tone"])
        assertEquals("구어체", map["formality"])
        assertEquals("짧음", map["sentence_length"])
        assertEquals("단문 위주", map["sentence_structure"])
        assertEquals("편안하고 일상적인 단어", map["lexical_choice"])
        assertEquals("약간 무기력", map["emotional_tone"])
        assertEquals("거의 없음", map["irony_or_sarcasm"])
        assertEquals("반말", map["slang_or_dialect"])
        assertEquals("보통", map["pacing"])

        // 리스트 필드들
        @Suppress("UNCHECKED_CAST")
        val endings = map["sentence_endings"] as List<*>
        assertTrue(endings.contains("~다"))
        assertTrue(endings.contains("~네"))
        assertTrue(endings.contains("~야"))

        @Suppress("UNCHECKED_CAST")
        val phrases = map["common_phrases"] as List<*>
        assertTrue(phrases.contains("그냥"))
        assertTrue(phrases.contains("아무래도"))
    }

    @Test
    fun extractMergedText_returnsDiaryWhenResultDiaryPresent() {
        // given
        val resultObj = JsonObject().apply {
            add("diary", JsonPrimitive("오늘의 일기입니다."))
        }
        val root = JsonObject().apply {
            add("result", resultObj)
        }

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("오늘의 일기입니다.", text)
    }

    @Test
    fun extractMergedText_returnsMergedContentWhenNoDiary() {
        // given
        val inner = JsonObject().apply {
            add("merged_content", JsonPrimitive("중간 병합 결과입니다."))
        }
        val root = JsonObject().apply {
            add("merged_content", inner)
        }

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("중간 병합 결과입니다.", text)
    }

    @Test
    fun extractMergedText_prefersDiaryOverMergedContentWhenBothExist() {
        // given
        val resultObj = JsonObject().apply {
            add("diary", JsonPrimitive("최종 일기 우선"))
        }
        val mergedInner = JsonObject().apply {
            add("merged_content", JsonPrimitive("이건 무시되어야 함"))
        }

        val root = JsonObject().apply {
            add("result", resultObj)
            add("merged_content", mergedInner)
        }

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("최종 일기 우선", text)
    }

    @Test
    fun extractMergedText_returnsEmptyStringWhenNoValidFields() {
        // given
        val root = JsonObject().apply {
            add("something_else", JsonPrimitive("값"))
        }

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("", text)
    }

    @Test
    fun extractMergedText_returnsEmptyStringWhenJsonEmpty() {
        // given
        val root = JsonObject()

        // when
        val text = MemoMergeUtils.extractMergedText(root)

        // then
        assertEquals("", text)
    }

}

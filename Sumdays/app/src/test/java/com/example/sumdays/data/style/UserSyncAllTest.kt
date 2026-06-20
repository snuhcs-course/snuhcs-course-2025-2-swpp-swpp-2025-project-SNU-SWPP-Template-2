package com.example.sumdays.sync_test

import com.example.sumdays.data.sync.*
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.EmotionAnalysis
import com.example.sumdays.data.Insights
import com.example.sumdays.data.Memo
import com.example.sumdays.data.SummaryDetails
import com.example.sumdays.data.UserStyle
import com.example.sumdays.data.WeekSummary
import org.junit.Assert.*
import org.junit.Test

class UserSyncAllTest {

    // -------------------- buildSyncRequest 테스트 ------------------------

    @Test
    fun buildSyncRequest_fullPayload() {
        val memoDel = listOf(1)
        val styleDel = listOf(2L)
        val entryDel = listOf("2025-01-01")
        val weekDel = listOf("2025-01-07")

        val editedMemos = listOf(
            Memo(
                id = 10,
                content = "c",
                timestamp = "t",
                date = "2025-01-03",
                order = 1,
                type = "text",
                isEdited = true,
                isDeleted = false
            )
        )

        val editedStyles = listOf(
            UserStyle(
                styleId = 7,
                styleName = "S",
                styleVector = listOf(1f, 2f),
                styleExamples = listOf("ex"),
                stylePrompt = dummyPrompt(),
                sampleDiary = "d"
            )
        )

        val editedEntries = listOf(
            DailyEntry(
                date = "2025-01-04",
                diary = "d",
                keywords = "k",
                aiComment = "a",
                emotionScore = 0.4,
                emotionIcon = "🙂",
                themeIcon = "🌙",
                isEdited = true,
                isDeleted = false
            )
        )

        val editedSummaries = listOf(
            WeekSummary(
                startDate = "2025-01-08",
                endDate = "2025-01-14",
                diaryCount = 3,
                emotionAnalysis =  dummyEmotionAnalysis(),
                highlights = emptyList(),
                insights = dummyInsights(),
                summary = dummySummary()
            )
        )

        val req = buildSyncRequest(
            memoDel, styleDel, entryDel, weekDel,
            editedMemos, editedStyles, editedEntries, editedSummaries
        )

        assertNotNull(req.deleted)
        assertNotNull(req.edited)
        /*
        assertEquals(1, req.deleted!!.memo!!.size)
        assertEquals(2L, req.deleted!!.userStyle!!.first())
        assertEquals("2025-01-03", req.edited!!.memo!!.first().date)
        assertEquals("S", req.edited!!.userStyle!!.first().styleName)
        */
    }

    @Test
    fun buildSyncRequest_emptyPayload() {
        val req = buildSyncRequest(
            emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList()
        )

        assertNull(req.deleted)
        assertNull(req.edited)
    }

    // -------------------- BackupScheduler 테스트 (JVM에서 가능한 범위) ------------------------


    // -------------------- DTO 포맷 테스트 ------------------------

    @Test
    fun syncDto_formatCheck() {
        val payload = MemoPayload(
            room_id = 1,
            content = "hello",
            timestamp = "10:00",
            date = "2025-01-01",
            memo_order = 0,
            type = "text"
        )

        assertEquals("hello", payload.content)
        assertEquals("2025-01-01", payload.date)
        assertEquals(1, payload.room_id)
    }

    @Test
    fun syncResponse_basic() {
        val resp = SyncResponse("success", "ok")
        assertEquals("success", resp.status)
        assertEquals("ok", resp.message)
    }

    // -------------------- Dummy Helper ------------------------
    private fun dummyEmotionAnalysis() =
        EmotionAnalysis(
            distribution = emptyMap(),
            dominantEmoji = "",
            emotionScore = 0.0,
            trend = ""
        )
    private fun dummyPrompt() = com.example.sumdays.data.style.StylePrompt(
        character_concept = "",
        emotional_tone = "",
        formality = "",
        lexical_choice = "",
        pacing = "",
        punctuation_style = "",
        sentence_endings = emptyList(),
        sentence_length = "",
        sentence_structure = "",
        special_syntax = "",
        speech_quirks = "",
        tone = ""
    )

    private fun dummyInsights() =
        Insights(
            advice = "a",
            emotionCycle = "c"
        )

    private fun dummySummary() =
        SummaryDetails(
            emergingTopics = emptyList(),
            overview = "",
            title = ""
        )
}

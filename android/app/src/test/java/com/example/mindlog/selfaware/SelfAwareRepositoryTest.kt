package com.example.mindlog.selfaware

import com.example.mindlog.core.common.Paged
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.selfaware.data.api.SelfAwareApi
import com.example.mindlog.features.selfaware.data.dto.AnswerRequest
import com.example.mindlog.features.selfaware.data.dto.AnswerResponse
import com.example.mindlog.features.selfaware.data.dto.CategoryResponse
import com.example.mindlog.features.selfaware.data.dto.QACursorResponse
import com.example.mindlog.features.selfaware.data.dto.QAResponse
import com.example.mindlog.features.selfaware.data.dto.QuestionResponse
import com.example.mindlog.features.selfaware.data.dto.TopValueScoresResponse
import com.example.mindlog.features.selfaware.data.dto.ValueMapResponse
import com.example.mindlog.features.selfaware.data.dto.ValueScoreResponse
import com.example.mindlog.features.selfaware.data.mapper.SelfAwareMapper
import com.example.mindlog.features.selfaware.data.repository.SelfAwareRepositoryImpl
import com.example.mindlog.features.selfaware.domain.model.Answer
import com.example.mindlog.features.selfaware.domain.model.CategoryScore
import com.example.mindlog.features.selfaware.domain.model.PersonalityInsight
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.model.Question
import com.example.mindlog.features.selfaware.domain.model.TopValueScores
import com.example.mindlog.features.selfaware.domain.model.ValueMap
import com.example.mindlog.features.selfaware.domain.model.ValueScore
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.eq
import org.mockito.kotlin.verifyNoInteractions
import java.time.LocalDate
import kotlin.test.Test


class SelfAwareRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: SelfAwareApi
    private lateinit var mapper: SelfAwareMapper
    private lateinit var repo: SelfAwareRepositoryImpl
    private lateinit var dispatcherProvider: TestDispatcherProvider

    @Before
    fun setup() {
        api = mock(SelfAwareApi::class.java)
        mapper = mock(SelfAwareMapper::class.java)
        dispatcherProvider =  TestDispatcherProvider(mainDispatcherRule.testDispatcher)
        repo = SelfAwareRepositoryImpl(api, mapper, dispatcherProvider)
    }

    // ---------------------------
    // getTodayQA
    // ---------------------------
    @Test
    fun `getTodayQA success maps to domain`() = runTest {
        val date = LocalDate.of(2025, 10, 25)

        // API DTO 준비
        val questionDto = QuestionResponse(
            id = 13,
            questionType = "single_category",
            text = "무엇이 의미 있었나요?",
            createdAt = "2025-10-25T15:27:16"
        )
        val answerDto: AnswerResponse? = null
        val qaDto = QAResponse(question = questionDto, answer = answerDto)

        // Domain 예상
        val domainQa = QAItem(
            question = Question(
                id = 13,
                type = "single_category",
                text = "무엇이 의미 있었나요?",
                createdAt = LocalDate.of(2025, 10, 25)
            ),
            answer = null
        )

        `when`(api.getTodayQA(date.toString())).thenReturn(qaDto)
        `when`(mapper.toQAItem(qaDto)).thenReturn(domainQa)

        val res = repo.getTodayQA(date)

        assertTrue(res is Result.Success)
        res as Result.Success
        assertEquals(13, res.data?.question?.id)
        verify(api).getTodayQA(eq(date.toString()))
        verify(mapper).toQAItem(eq(qaDto))
    }

    @Test
    fun `getTodayQA error wraps as Result_Error`() = runTest {
        val date = LocalDate.of(2025, 10, 25)

        // API에서 예외 발생시키기
        `when`(api.getTodayQA(date.toString())).thenThrow(RuntimeException("boom"))

        val res = repo.getTodayQA(date)

        assertTrue(res is Result.Error)
        res as Result.Error
        assertTrue(res.message?.contains("boom") == true)
        verify(api).getTodayQA(eq(date.toString()))
        verifyNoInteractions(mapper) // 매핑 단계까지 못 감
    }

    // ---------------------------
    // submitAnswer
    // ---------------------------
    @Test
    fun `submitAnswer success maps to domain Answer`() = runTest {
        val qid = 13
        val text = "오늘은 친구와 대화하며 마음이 편안해졌다."
        val req = AnswerRequest(questionId = qid, text = text)

        val answerDto = AnswerResponse(
            id = 6,
            questionId = qid,
            type = "text",
            text = text,
            createdAt = "2025-10-25T16:16:12",
            updatedAt = "2025-10-25T16:16:12"
        )
        val domainAnswer = Answer(
            id = 6, questionId = qid, type = "text", text = text,
            createdAt = LocalDate.of(2025, 10, 25),
            updatedAt = LocalDate.of(2025, 10, 25)
        )

        `when`(api.submitAnswer(req)).thenReturn(answerDto)
        `when`(mapper.toAnswer(answerDto)).thenReturn(domainAnswer)

        val res = repo.submitAnswer(qid, text)

        assertTrue(res is Result.Success)
        res as Result.Success
        assertEquals(6, res.data.id)
        verify(api).submitAnswer(eq(req))
        verify(mapper).toAnswer(eq(answerDto))
    }

    @Test
    fun `submitAnswer error`() = runTest {
        val qid = 99
        val text = "텍스트"

        `when`(api.submitAnswer(AnswerRequest(qid, text))).thenThrow(IllegalStateException("fail"))

        val res = repo.submitAnswer(qid, text)

        assertTrue(res is Result.Error)
        verify(api).submitAnswer(eq(AnswerRequest(qid, text)))
        verifyNoInteractions(mapper)
    }

    // ---------------------------
    // getQAHistory (paged)
    // ---------------------------
    @Test
    fun `getQAHistory success maps items and cursor`() = runTest {
        val limit = 10
        val cursor: Int? = 2

        val q1 = QuestionResponse(
            id = 1, questionType = "single", text = "Q1?", createdAt = "2025-10-20T10:00:00"
        )
        val a1 = AnswerResponse(
            id = 11, questionId = 1, type = "text", text = "A1",
            createdAt = "2025-10-20T11:00:00", updatedAt = "2025-10-20T11:00:00"
        )
        val q2 = QuestionResponse(
            id = 2, questionType = "single", text = "Q2?", createdAt = "2025-10-21T10:00:00"
        )

        val pageDto = QACursorResponse(
            items = listOf(
                QAResponse(q1, a1),
                QAResponse(q2, null),
            ),
            next_cursor = 5
        )

        val domain1 = QAItem(
            question = Question(1, "single", "Q1?", LocalDate.of(2025,10,20)),
            answer = Answer(11, 1, "text", "A1", LocalDate.of(2025,10,20), LocalDate.of(2025,10,20))
        )
        val domain2 = QAItem(
            question = Question(2, "single", "Q2?", LocalDate.of(2025,10,21)),
            answer = null
        )

        `when`(api.getQAHistory(limit = limit, cursor = cursor)).thenReturn(pageDto)
        `when`(mapper.toQAItem(pageDto.items[0])).thenReturn(domain1)
        `when`(mapper.toQAItem(pageDto.items[1])).thenReturn(domain2)

        val res = repo.getQAHistory(limit, cursor)

        assertTrue(res is Result.Success)
        res as Result.Success
        val paged: Paged<QAItem> = res.data
        assertEquals(2, paged.items.size)
        assertEquals(5, paged.cursor)
        assertEquals(limit, paged.size)

        verify(api).getQAHistory(limit = eq(limit), cursor = eq(cursor))
        verify(mapper).toQAItem(eq(pageDto.items[0]))
        verify(mapper).toQAItem(eq(pageDto.items[1]))
    }

    @Test
    fun `getQAHistory error`() = runTest {
        `when`(api.getQAHistory(limit = 10, cursor = null)).thenThrow(RuntimeException("network"))

        val res = repo.getQAHistory(limit = 10, cursor = null)

        assertTrue(res is Result.Error)
        verify(api).getQAHistory(limit = eq(10), cursor = eq(null))
        verifyNoInteractions(mapper)
    }

    // ---------------------------
    // getTopValueScores
    // ---------------------------
    @Test
    fun `getTopValueScores success`() = runTest {
        val dto = TopValueScoresResponse(
            valueScores = listOf(
                ValueScoreResponse("성장", 82f),
                ValueScoreResponse("자유", 74f),
            )
        )
        val domain = TopValueScores(
            valueScores = listOf(
                ValueScore("성장", 82f),
                ValueScore("자유", 74f),
            )
        )

        `when`(api.getTopValueScores()).thenReturn(dto)
        `when`(mapper.toTopValueScores(dto)).thenReturn(domain)

        val res = repo.getTopValueScores()

        assertTrue(res is Result.Success)
        res as Result.Success
        assertEquals(2, res.data.valueScores.size)
        verify(api).getTopValueScores()
        verify(mapper).toTopValueScores(eq(dto))
    }

    // ---------------------------
    // getValueMap
    // ---------------------------
    @Test
    fun `getValueMap success`() = runTest {
        val dto = ValueMapResponse(
            categoryScores = listOf(
                CategoryResponse("Growth", "성장", 80),
                CategoryResponse("Freedom", "자유", 70),
            ),
            updatedAt = "2025-10-25T22:00:00"
        )
        val domain = ValueMap(
            categoryScores = listOf(
                CategoryScore("Growth", "성장", 80),
                CategoryScore("Freedom", "자유", 70),
            ),
            updatedAt = LocalDate.of(2025, 10, 25)
        )

        `when`(api.getValueMap()).thenReturn(dto)
        `when`(mapper.toValueMap(dto)).thenReturn(domain)

        val res = repo.getValueMap()

        assertTrue(res is Result.Success)
        res as Result.Success
        assertEquals(2, res.data.categoryScores.size)
        verify(api).getValueMap()
        verify(mapper).toValueMap(eq(dto))
    }
}
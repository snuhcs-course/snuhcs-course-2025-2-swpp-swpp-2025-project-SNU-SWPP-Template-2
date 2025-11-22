package com.example.mindlog.features.selfaware.domain.usecase

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.selfaware.domain.model.*
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.eq
import java.time.LocalDate

class SelfAwareUseCasesTest {

    private lateinit var repo: SelfAwareRepository

    private lateinit var getTodayQAUseCase: GetTodayQAUseCase
    private lateinit var submitAnswerUseCase: SubmitAnswerUseCase
    private lateinit var getQAHistoryUseCase: GetQAHistoryUseCase
    private lateinit var getTopValueScoresUseCase: GetTopValueScoresUseCase
    private lateinit var getValueMapUseCase: GetValueMapUseCase

    @Before
    fun setup() {
        repo = mock(SelfAwareRepository::class.java)

        getTodayQAUseCase = GetTodayQAUseCase(repo)
        submitAnswerUseCase = SubmitAnswerUseCase(repo)
        getQAHistoryUseCase = GetQAHistoryUseCase(repo)
        getTopValueScoresUseCase = GetTopValueScoresUseCase(repo)
        getValueMapUseCase = GetValueMapUseCase(repo)
    }

    // ------------------------------
    // getTodayQA
    // ------------------------------
    @Test
    fun `getTodayQAUseCase delegates to repository`() = runTest {
        val today = LocalDate.of(2025, 10, 25)
        val expected = Result.Success(QAItem(
            question = Question(1, "single", "질문", today),
            answer = null
        ))
        `when`(repo.getTodayQA(today)).thenReturn(expected)

        val result = getTodayQAUseCase(today)

        assertSame(expected, result)
        verify(repo).getTodayQA(eq(today))
        verifyNoMoreInteractions(repo)
    }

    // ------------------------------
    // submitAnswer
    // ------------------------------
    @Test
    fun `submitAnswerUseCase delegates to repository`() = runTest {
        val expected = Result.Success(
            Answer(10, 1, "text", "내용", LocalDate.now(), LocalDate.now())
        )
        `when`(repo.submitAnswer(1, "내용")).thenReturn(expected)

        val result = submitAnswerUseCase(1, "내용")

        assertSame(expected, result)
        verify(repo).submitAnswer(eq(1), eq("내용"))
        verifyNoMoreInteractions(repo)
    }

    // ------------------------------
    // getQAHistory
    // ------------------------------
    @Test
    fun `getQAHistoryUseCase delegates to repository`() = runTest {
        val expected = Result.Success(
            com.example.mindlog.core.common.Paged(emptyList<QAItem>(), cursor = 5, size = 10)
        )
        `when`(repo.getQAHistory(10, null)).thenReturn(expected)

        val result = getQAHistoryUseCase(10, null)

        assertSame(expected, result)
        verify(repo).getQAHistory(eq(10), eq(null))
        verifyNoMoreInteractions(repo)
    }

    // ------------------------------
    // getTopValueScores
    // ------------------------------
    @Test
    fun `getTopValueScoresUseCase delegates to repository`() = runTest {
        val expected = Result.Success(
            TopValueScores(listOf(ValueScore("성장", 90f)))
        )
        `when`(repo.getTopValueScores()).thenReturn(expected)

        val result = getTopValueScoresUseCase(Unit)

        assertSame(expected, result)
        verify(repo).getTopValueScores()
        verifyNoMoreInteractions(repo)
    }

    // ------------------------------
    // getValueMap
    // ------------------------------
    @Test
    fun `getValueMapUseCase delegates to repository`() = runTest {
        val expected = Result.Success(
            ValueMap(listOf(CategoryScore("Growth", "성장", 80)), LocalDate.now())
        )
        `when`(repo.getValueMap()).thenReturn(expected)

        val result = getValueMapUseCase(Unit)

        assertSame(expected, result)
        verify(repo).getValueMap()
        verifyNoMoreInteractions(repo)
    }
}
package com.example.mindlog.statistics

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.statistics.domain.model.Emotion
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import com.example.mindlog.features.statistics.domain.usecase.GetEmotionRatesUseCase
import com.example.mindlog.features.statistics.domain.usecase.GetJournalStatisticsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsUseCasesTest {

    private lateinit var repo: StatisticsRepository
    private lateinit var getEmotionRates: GetEmotionRatesUseCase
    private lateinit var getJournalStatistics: GetJournalStatisticsUseCase

    @Before
    fun setUp() {
        repo = mock()
        getEmotionRates = GetEmotionRatesUseCase(repo)
        getJournalStatistics = GetJournalStatisticsUseCase(repo)
    }

    @Test
    fun `GetEmotionRatesUseCase delegates to repository and returns success`() = runTest {
        // given
        val start = "2025-10-01"
        val end = "2025-10-07"
        val rates = listOf(
            EmotionRate(Emotion.HAPPY, count = 20, percentage = 0.20f),
            EmotionRate(Emotion.SAD, count = 10, percentage = 0.10f)
        )
        whenever(repo.getEmotionRates(eq(start), eq(end))).thenReturn(Result.Success(rates))

        // when
        val res = getEmotionRates(start, end)

        // then
        verify(repo).getEmotionRates(eq(start), eq(end))
        assertEquals(Result.Success(rates), res)
    }

    @Test
    fun `GetEmotionRatesUseCase propagates repository error`() = runTest {
        // given
        val start = "2025-10-01"
        val end = "2025-10-07"
        whenever(repo.getEmotionRates(any(), any())).thenReturn(Result.Error(404, "network error"))

        // when
        val res = getEmotionRates(start, end)

        // then
        verify(repo).getEmotionRates(eq(start), eq(end))
        assertEquals("network error", (res as Result.Error).message)
    }

    @Test
    fun `GetJournalStatisticsUseCase delegates to repository and returns success`() = runTest {
        // given
        val start = "2025-11-01"
        val end = "2025-11-07"
        val stats = JournalStatistics(
            EmotionTrends = listOf(EmotionTrend(Emotion.CALM, listOf(1, 2, 3))),
            EmotionEvents = emptyList(),
            JournalKeywords = listOf(JournalKeyword("운동", 3))
        )
        whenever(repo.getJournalStatisics(eq(start), eq(end))).thenReturn(Result.Success(stats))

        // when
        val res = getJournalStatistics(start, end)

        // then
        verify(repo).getJournalStatisics(eq(start), eq(end))
        assertEquals(Result.Success(stats), res)
    }

    @Test
    fun `GetJournalStatisticsUseCase propagates repository error`() = runTest {
        // given
        val start = "2025-11-01"
        val end = "2025-11-07"
        whenever(repo.getJournalStatisics(any(), any())).thenReturn(Result.Error(500,"server error"))

        // when
        val res = getJournalStatistics(start, end)

        // then
        verify(repo).getJournalStatisics(eq(start), eq(end))
        assertEquals("server error", (res as Result.Error).message)
    }
}
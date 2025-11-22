package com.example.mindlog.statistics

import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.statistics.domain.model.*
import com.example.mindlog.features.statistics.domain.usecase.GetEmotionRatesUseCase
import com.example.mindlog.features.statistics.domain.usecase.GetJournalStatisticsUseCase
import com.example.mindlog.features.statistics.presentation.StatisticsViewModel
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getEmotionRatesUseCase: GetEmotionRatesUseCase
    private lateinit var getJournalStatisticsUseCase: GetJournalStatisticsUseCase
    private lateinit var vm: StatisticsViewModel


    @Before
    fun setUp() {
        val testDispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)

        getEmotionRatesUseCase = mock()
        getJournalStatisticsUseCase = mock()
        vm = StatisticsViewModel(
            getEmotionRatesUseCase,
            getJournalStatisticsUseCase,
            testDispatcherProvider
        )
    }

    @Test
    fun `load() picks top emotion from rates when no previous selection`() = runTest {
        // given
        val start = LocalDate.of(2025, 10, 1)
        val end = LocalDate.of(2025, 10, 7)
        vm.setDateRange(start, end)

        val rates = listOf(
            EmotionRate(Emotion.SAD, count = 10, percentage = 0.10f),
            EmotionRate(Emotion.HAPPY, count = 20, percentage = 0.20f), // top
            EmotionRate(Emotion.CALM, count = 5, percentage = 0.05f),
        )
        val stats = JournalStatistics(
            EmotionTrends = listOf(
                EmotionTrend(Emotion.CALM, trend = listOf(1,2,3))
            ),
            EmotionEvents = listOf(
                EmotionEvent(Emotion.HAPPY, events = listOf("기쁜 일")),
                EmotionEvent(Emotion.SAD, events = listOf("슬픈 일"))
            ),
            JournalKeywords = listOf(JournalKeyword("운동", 3))
        )

        whenever(getEmotionRatesUseCase(any(), any())).thenReturn(Result.Success(rates))
        whenever(getJournalStatisticsUseCase(any(), any())).thenReturn(Result.Success(stats))

        // when
        vm.load()
        advanceUntilIdle()

        // then
        val state = vm.state.value
        print(state)
        assertEquals(false, state.isLoading)
        assertEquals(rates, state.emotionRatios)
        assertEquals(stats, state.statistics)
        assertEquals(Emotion.HAPPY, state.selectedEmotion) // top from ratio
        assertEquals(listOf("기쁜 일"), state.emotionEvents) // filtered by selected emotion
        assertEquals(listOf(EmotionTrend(Emotion.CALM, listOf(1,2,3))), state.emotionTrends)
        assertEquals(listOf(JournalKeyword("운동", 3)), state.journalKeywords)
    }

    @Test
    fun `load() falls back to first trend emotion when rates empty`() = runTest {
        // given
        val start = LocalDate.of(2025, 10, 1)
        val end = LocalDate.of(2025, 10, 7)
        vm.setDateRange(start, end)

        val rates = emptyList<EmotionRate>()
        val stats = JournalStatistics(
            EmotionTrends = listOf(
                EmotionTrend(Emotion.ANXIOUS, trend = listOf(9,9,9)) // first trend
            ),
            EmotionEvents = listOf(
                EmotionEvent(Emotion.ANXIOUS, events = listOf("불안했던 이유"))
            ),
            JournalKeywords = emptyList()
        )

        whenever(getEmotionRatesUseCase(any(), any())).thenReturn(Result.Success(rates))
        whenever(getJournalStatisticsUseCase(any(), any())).thenReturn(Result.Success(stats))

        // when
        vm.load()
        advanceUntilIdle()

        // then
        val state = vm.state.value
        assertEquals(Emotion.ANXIOUS, state.selectedEmotion)
        assertEquals(listOf("불안했던 이유"), state.emotionEvents)
    }

    @Test
    fun `setEmotion updates only UI-derivatives without reloading data`() = runTest {
        // given existing stats in state
        val stats = JournalStatistics(
            EmotionTrends = listOf(
                EmotionTrend(Emotion.SAD, listOf(5,4,3))
            ),
            EmotionEvents = listOf(
                EmotionEvent(Emotion.SAD, listOf("시험에서 실수"))
            ),
            JournalKeywords = listOf(JournalKeyword("시험", 1))
        )

        whenever(getEmotionRatesUseCase(any(), any())).thenReturn(Result.Success(emptyList()))
        whenever(getJournalStatisticsUseCase(any(), any())).thenReturn(Result.Success(stats))
        vm.load()
        advanceUntilIdle()

        // when
        vm.setEmotion(Emotion.SAD)
        advanceUntilIdle()

        // then
        val state = vm.state.value
        assertEquals(Emotion.SAD, state.selectedEmotion)
        assertEquals(listOf("시험에서 실수"), state.emotionEvents)
        assertEquals(listOf(EmotionTrend(Emotion.SAD, listOf(5,4,3))), state.emotionTrends)
        assertEquals(listOf(JournalKeyword("시험", 1)), state.journalKeywords)
    }

    @Test
    fun `setDateRange only updates period - load is called by UI later`() = runTest {
        // given initial range
        val start = LocalDate.of(2025, 11, 1)
        val end = LocalDate.of(2025, 11, 7)

        // when
        vm.setDateRange(start, end)
        advanceUntilIdle()

        // then
        val state = vm.state.value
        assertEquals(start, state.startDate)
        assertEquals(end, state.endDate)
        // no auto-loading here
        assertTrue(state.emotionRatios.isEmpty())
        assertEquals(null, state.statistics)
    }

    @Test
    fun `error from either usecase is surfaced in state`() = runTest {
        whenever(getEmotionRatesUseCase(any(), any())).thenReturn(Result.Error(404, "Get EmotionRate Error"))
        whenever(getJournalStatisticsUseCase(any(), any())).thenReturn(Result.Success(
            JournalStatistics(emptyList(), emptyList(), emptyList())
        ))

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Get EmotionRate Error", state.error)
    }
}
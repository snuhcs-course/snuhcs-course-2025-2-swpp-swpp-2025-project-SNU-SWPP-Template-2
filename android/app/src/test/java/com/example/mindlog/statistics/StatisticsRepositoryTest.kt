package com.example.mindlog.statistics

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.JournalListResponse
import com.example.mindlog.features.statistics.data.api.StatisticsApi
import com.example.mindlog.features.statistics.data.dto.EmotionRateItem
import com.example.mindlog.features.statistics.data.dto.EmotionRatesResponse
import com.example.mindlog.features.statistics.data.mapper.StatisticsMapper
import com.example.mindlog.features.statistics.data.repository.StatisticsRepositoryImpl
import com.example.mindlog.features.statistics.domain.model.Emotion
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var statisticsApi: StatisticsApi
    private lateinit var journalApi: JournalApi
    private lateinit var mapper: StatisticsMapper
    private lateinit var repo: StatisticsRepositoryImpl

    private lateinit var dispatcherProvider: TestDispatcherProvider

    @Before
    fun setUp() {
        statisticsApi = mock()
        journalApi = mock()
        mapper = mock()
        dispatcherProvider =  TestDispatcherProvider(mainDispatcherRule.testDispatcher)
        repo = StatisticsRepositoryImpl(statisticsApi, journalApi, mapper, dispatcherProvider)
    }

    @Test
    fun `getEmotionRates maps api response and returns Success`() = runTest{
        // given
        val start = "2025-10-01"
        val end = "2025-10-07"
        val dto = EmotionRatesResponse(
            totalCount = 100,
            statistics = listOf(
                EmotionRateItem(emotion = "happy", count = 20, percentage = 10.0f),
                EmotionRateItem(emotion = "sad", count = 10, percentage = 5.0f)
            )
        )
        whenever(statisticsApi.getEmotionRates(eq(start), eq(end))).thenReturn(dto)

        val mapped0 = EmotionRate(Emotion.HAPPY, count = 20, percentage = 0.10f)
        val mapped1 = EmotionRate(Emotion.SAD, count = 10, percentage = 0.05f)
        whenever(mapper.toEmotionRate(dto.statistics[0])).thenReturn(mapped0)
        whenever(mapper.toEmotionRate(dto.statistics[1])).thenReturn(mapped1)

        // when
        val res = repo.getEmotionRates(start, end)

        // then
        verify(statisticsApi).getEmotionRates(eq(start), eq(end))
        verify(mapper).toEmotionRate(dto.statistics[0])
        verify(mapper).toEmotionRate(dto.statistics[1])

        assertTrue(res is Result.Success)
        val data = (res as Result.Success).data
        assertEquals(listOf(mapped0, mapped1), data)
    }

    @Test
    fun `getEmotionRates returns Error when api throws`() = runTest {
        // given
        whenever(statisticsApi.getEmotionRates(any(), any())).thenThrow(RuntimeException("boom"))

        // when
        val res = repo.getEmotionRates("2025-10-01", "2025-10-07")

        // then
        assertTrue(res is Result.Error)
        res as Result.Error
        TestCase.assertTrue(res.message?.contains("boom") == true)
        verify(statisticsApi).getEmotionRates(eq("2025-10-01"), eq("2025-10-07"))
        verifyNoInteractions(mapper)
    }

    @Test
    fun `getJournalStatisics returns Success when mapper succeeds`() = runTest{
        // given: one-page paging to avoid depending on response concrete class
        // Use real dummy response and items.
        val items = listOf(
            JournalItemResponse(
                id = 1,
                title = "테스트 일기 1",
                content = "오늘은 테스트 데이터를 작성했다.",
                emotions = emptyList(),
                gratitude = "감사합니다",
                imageS3Keys = null,
                createdAt = "2025-11-01T10:00:00Z",
                keywords = emptyList()
            ),
            JournalItemResponse(
                id = 2,
                title = "테스트 일기 2",
                content = "테스트 중 두 번째 일기입니다.",
                emotions = emptyList(),
                gratitude = "소중한 하루",
                imageS3Keys = null,
                createdAt = "2025-11-02T10:00:00Z",
                keywords = emptyList()
            )
        )
        val fakeResponse = JournalListResponse(
            items = items,
            nextCursor = null
        )
        whenever(
            journalApi.searchJournals(
                startDate = eq("2025-11-01"),
                endDate = eq("2025-11-07"),
                title = isNull(),
                limit = eq(50),
                cursor = isNull()
            )
        ).thenReturn(fakeResponse)

        val mapped = JournalStatistics(emptyList(), emptyList(), emptyList())
        whenever(mapper.toJournalStatistics(items)).thenReturn(mapped)

        // when
        val res = repo.getJournalStatisics("2025-11-01", "2025-11-07")
        print(res)

        // then
        verify(journalApi).searchJournals(
            startDate = eq("2025-11-01"),
            endDate = eq("2025-11-07"),
            title = eq(null),
            limit = eq(50),
            cursor = eq(null)
        )
        verify(mapper).toJournalStatistics(eq(items))
        assertEquals(Result.Success(mapped), res)
    }

    @Test
    fun `getJournalStatisics returns Error when api throws`() = runTest {
        whenever(
            journalApi.searchJournals(
                any(),
                any(),
                isNull(),
                any(),
                isNull()
            )
        ).thenThrow(IllegalStateException("journal api down"))

        val res = repo.getJournalStatisics("2025-11-01", "2025-11-07")

        assertTrue(res is Result.Error)
        assertTrue((res as Result.Error).message?.contains("journal api down") == true)
    }
}
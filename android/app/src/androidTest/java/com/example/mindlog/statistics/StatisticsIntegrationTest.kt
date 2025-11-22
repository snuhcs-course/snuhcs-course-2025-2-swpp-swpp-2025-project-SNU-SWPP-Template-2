package com.example.mindlog.statistics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.statistics.data.api.StatisticsApi
import com.example.mindlog.features.statistics.data.mapper.StatisticsMapper
import com.example.mindlog.features.statistics.data.repository.StatisticsRepositoryImpl
import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import com.example.mindlog.utils.TestDispatcherProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StatisticsIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repo: StatisticsRepository

    @Before
    fun setUp() {
        hiltRule.inject()

        mockWebServer = MockWebServer().apply {
            dispatcher = TestStatisticsDispatcher()
            start(0)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val statisticsApi = retrofit.create(StatisticsApi::class.java)
        val journalApi = retrofit.create(JournalApi::class.java)

        repo = StatisticsRepositoryImpl(
            statisticsApi = statisticsApi,
            journalApi = journalApi,
            mapper = StatisticsMapper(),
            dispatcher = TestDispatcherProvider()
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getEmotionRates_returns_success_and_maps_domain() = runBlocking {
        val start = "2025-10-09"
        val end = "2025-11-09"

        val res = repo.getEmotionRates(start, end)
        assertTrue(res is Result.Success)

        val data = (res as Result.Success).data
        assertEquals(10, data.size)
        assertEquals("HAPPY", data.first().emotion.name)
    }

    @Test
    fun getEmotionRates_sends_expected_query_params() = runBlocking {
        val start = "2025-10-09"
        val end = "2025-11-09"

        // call through repository (which calls the API)
        repo.getEmotionRates(start, end)

        val req = mockWebServer.takeRequest()
        assertEquals("GET", req.method)
        val url = req.requestUrl!!
        val pathWithQuery = url.encodedPath + "?" + (url.query ?: "")
        assert(pathWithQuery.startsWith("/statistics/emotion-rate"))
        assert(pathWithQuery.contains("start_date=$start"))
        assert(pathWithQuery.contains("end_date=$end"))
    }

    @Test
    fun getJournalStatistics_returns_success_and_non_empty_keywords() = runBlocking {
        val start = "2025-10-09"
        val end = "2025-11-09"

        val res = repo.getJournalStatisics(start, end) // ← Repository 오탈자 그대로 사용
        assertTrue(res is Result.Success)

        val stats = (res as Result.Success).data
        // 최소 유효성: /journal/search 더미 응답(Dispatcher) 기준으로 키워드가 비어 있지 않아야 함
        assertTrue("JournalKeywords should not be empty", stats.JournalKeywords.isNotEmpty())

        // (매퍼 구현 따라 켤 수 있는 선택 검증)
        assertTrue(stats.EmotionEvents.isNotEmpty())
        assertTrue(stats.EmotionTrends.isNotEmpty())
    }
}
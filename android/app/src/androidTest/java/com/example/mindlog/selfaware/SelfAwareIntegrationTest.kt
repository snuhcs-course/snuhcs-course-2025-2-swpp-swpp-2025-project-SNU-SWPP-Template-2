package com.example.mindlog.selfaware

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.features.selfaware.data.api.SelfAwareApi
import com.example.mindlog.features.selfaware.data.mapper.SelfAwareMapper
import com.example.mindlog.features.selfaware.data.repository.SelfAwareRepositoryImpl
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import com.example.mindlog.utils.TestDispatcherProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import com.example.mindlog.core.common.Result


@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SelfAwareIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repo: SelfAwareRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        mockWebServer = MockWebServer().apply {
            dispatcher = TestSelfAwareDispatcher()
            start(8000)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SelfAwareApi::class.java)

        repo = SelfAwareRepositoryImpl(
            api = api,
            mapper = SelfAwareMapper(),
            dispatcher = TestDispatcherProvider()
        )
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetTodayQuestionWithAnswer() = kotlinx.coroutines.runBlocking {
        val res = repo.getTodayQA(LocalDate.of(2025, 10, 25))

        assertTrue(res is Result.Success)
        val qa = (res as Result.Success).data
        assertNotNull(qa)
        assertNotNull(qa.question)
        assertEquals(13, qa.question!!.id)
        assertEquals(true, qa.answer != null)
        assertEquals(6, qa.answer!!.id)
    }

    @Test
    fun testSubmitAnswerSuccess() = kotlinx.coroutines.runBlocking {
        val res = repo.submitAnswer(questionId = 13, answer = "테스트 답변 본문")
        assertTrue(res is Result.Success)
        val answer = (res as Result.Success).data
        assertEquals(100, answer.id)
        assertEquals(13, answer.questionId)
        assertEquals("테스트 답변 본문", answer.text)
    }

    @Test
    fun testFetchValueMapCategoryScores() = kotlinx.coroutines.runBlocking {
        val res = repo.getValueMap()
        assertTrue(res is Result.Success)
        val map = (res as Result.Success).data
        assertEquals(5, map.categoryScores.size)
        assertEquals("불안정성", map.categoryScores[0].categoryKo)
        assertEquals(72, map.categoryScores[0].score)
    }

    @Test
    fun testFetchTopValueScores() = kotlinx.coroutines.runBlocking {
        val res = repo.getTopValueScores()
        assertTrue(res is Result.Success)
        val top = (res as Result.Success).data
        assertEquals(5, top.valueScores.size)
        assertEquals("성장", top.valueScores[0].value)
    }


    @Test
    fun testFetchQAHistoryFirstPage() = kotlinx.coroutines.runBlocking {
        val res = repo.getQAHistory(limit = 10, cursor = null)
        assertTrue(res is Result.Success)
        val paged = (res as Result.Success).data
        assertEquals(1, paged.items.size)
        assertEquals(2, paged.cursor)
        assertEquals("Q1", paged.items.first().question!!.text)
        assertEquals("A1", paged.items.first().answer!!.text)
    }
}
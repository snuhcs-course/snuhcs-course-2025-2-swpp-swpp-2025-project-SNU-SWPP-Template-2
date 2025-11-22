package com.example.mindlog.analysis

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.analysis.data.api.AnalysisApi
import com.example.mindlog.features.analysis.data.mapper.AnalysisMapper
import com.example.mindlog.features.analysis.data.repository.AnalysisRepositoryImpl
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.analysis.domain.repository.AnalysisRepository
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
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AnalysisIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repo: AnalysisRepository

    @Before
    fun setUp() {
        hiltRule.inject()

        mockWebServer = MockWebServer().apply {
            dispatcher = TestAnalysisDispatcher()
            start(8001)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(AnalysisApi::class.java)

        repo = AnalysisRepositoryImpl(
            api = api,
            mapper = AnalysisMapper(),
            dispatcher = TestDispatcherProvider()
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getUserType_returnsSuccessAndMappedDomainModel() = kotlinx.coroutines.runBlocking {
        val res = repo.getUserType()

        assertTrue(res is Result.Success)
        val data = (res as Result.Success<UserType>).data
        assertNotNull(data)
        // 값 상세 검증은 모델 필드 이름에 맞춰 추가하면 됨
        // e.g. assertEquals("탐험가형", data.userTypeKo)
    }

    @Test
    fun getComprehensiveAnalysis_returnsSuccessAndDomainModel() = kotlinx.coroutines.runBlocking {
        val res = repo.getComprehensiveAnalysis()

        assertTrue(res is Result.Success)
        val data = (res as Result.Success<ComprehensiveAnalysis>).data
        assertNotNull(data)
        // 필요하면 conscientiousness 등 세부 문자열도 검증 가능
    }

    @Test
    fun getPersonalizedAdvice_returnsSuccessAndDomainModel() = kotlinx.coroutines.runBlocking {
        val res = repo.getPersonalizedAdvice()

        assertTrue(res is Result.Success)
        val data = (res as Result.Success<PersonalizedAdvice>).data
        assertNotNull(data)
        // e.g. assertEquals("오늘의 성장 포인트", data.adviceType)
    }
}
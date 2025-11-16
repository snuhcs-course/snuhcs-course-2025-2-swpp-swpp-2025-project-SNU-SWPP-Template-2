package com.example.sumdays.daily.diary

//import android.util.Log
//import com.example.sumdays.network.ApiClient
//import com.example.sumdays.network.ApiService
//import com.google.gson.Gson
//import com.google.gson.JsonObject
//import io.mockk.*
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.test.StandardTestDispatcher
//import kotlinx.coroutines.test.resetMain
//import kotlinx.coroutines.test.runTest
//import kotlinx.coroutines.test.setMain
//import org.junit.After
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import retrofit2.Response
//import java.io.IOException
//
//@ExperimentalCoroutinesApi
//class AnalysisRepositoryTest {
//
//    private lateinit var mockApiService: ApiService
//    private val analysisRepository = AnalysisRepository
//    private val testDispatcher = StandardTestDispatcher()
//    private val gson = Gson()
//
//    @Before
//    fun setUp() {
//        Dispatchers.setMain(testDispatcher)
//
//        // Log, ApiClient, DiaryRepository mocking
//        mockkStatic(Log::class)
//        every { Log.d(any(), any()) } returns 0
//        every { Log.e(any(), any<String>()) } returns 0
//        every { Log.e(any(), any(), any()) } returns 0
//        every { Log.w(any(), any<String>()) } returns 0
//
//        mockApiService = mockk()
//        mockkObject(DiaryRepository)
//        mockkObject(ApiClient)
//        every { ApiClient.api } returns mockApiService
//
//        analysisRepository.clearAllCache()
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//        unmockkObject(DiaryRepository, ApiClient)
//        unmockkStatic(Log::class)
//        unmockkAll()
//    }
//
//    @Test
//    fun `getAnalysis returns cached data when available`() {
//        val testDate = "2025-11-01"
//        val cachedBlock = AnalysisBlock(0.5, listOf("cache", "hit"))
//        val cachedResponse = AnalysisResponse(
//            aiComment = "Cached comment",
//            analysis = cachedBlock,
//            diary = "Cached diary",
//            entryDate = testDate,
//            icon = "💾",
//            userId = 456
//        )
//
//        analysisRepository.preloadCache(testDate, cachedResponse)
//
//        val result = analysisRepository.getAnalysis(testDate)
//
//        assertNotNull(result)
//        assertEquals(cachedResponse, result)
//    }
//
//    @Test
//    fun `requestAnalysis calls server and caches result`() = runTest(testDispatcher) {
//        val testDate = "2025-11-01"
//        val testDiary = "오늘의 일기 내용입니다."
//
//        val mockJson = """
//            {
//              "result": {
//                "ai_comment": "Mock AI Comment",
//                "analysis": { "emotion_score": 0.8, "keywords": ["server", "mock"] },
//                "diary": "$testDiary",
//                "entry_date": "$testDate",
//                "icon": "🌐",
//                "user_id": 789
//              }
//            }
//        """.trimIndent()
//        val response = Response.success(gson.fromJson(mockJson, JsonObject::class.java))
//
//        every { DiaryRepository.getDiary(testDate) } returns testDiary
//        coEvery { mockApiService.diaryAnalyze(any()) } returns response
//
//        val result = analysisRepository.requestAnalysis(testDate)
//
//        assertNotNull(result)
//        assertEquals("Mock AI Comment", result?.aiComment)
//        verify(exactly = 1) { DiaryRepository.getDiary(testDate) }
//        coVerify(exactly = 1) { mockApiService.diaryAnalyze(any()) }
//
//        val cached = analysisRepository.getAnalysis(testDate)
//        assertEquals(result, cached)
//    }
//
//    @Test
//    fun `requestAnalysis returns null if diary is missing`() = runTest(testDispatcher) {
//        val testDate = "2025-11-02"
//        every { DiaryRepository.getDiary(testDate) } returns null
//        coEvery { mockApiService.diaryAnalyze(any()) } throws Exception("Should not be called")
//
//        val result = analysisRepository.requestAnalysis(testDate)
//
//        assertNull(result)
//        verify(exactly = 1) { DiaryRepository.getDiary(testDate) }
//        coVerify(exactly = 0) { mockApiService.diaryAnalyze(any()) }
//    }
//
//    @Test
//    fun `requestAnalysis returns null on API failure`() = runTest(testDispatcher) {
//        val testDate = "2025-11-03"
//        val diary = "일기 내용 있음."
//        every { DiaryRepository.getDiary(testDate) } returns diary
//        coEvery { mockApiService.diaryAnalyze(any()) } throws IOException("Network error")
//
//        val result = analysisRepository.requestAnalysis(testDate)
//
//        assertNull(result)
//        verify(exactly = 1) { DiaryRepository.getDiary(testDate) }
//        coVerify(exactly = 1) { mockApiService.diaryAnalyze(any()) }
//        assertNull(analysisRepository.getAnalysis(testDate))
//    }
//
//    // ---- Helpers ----
//    private fun AnalysisRepository.preloadCache(date: String, data: AnalysisResponse) {
//        val field = AnalysisRepository::class.java.getDeclaredField("analysisCache")
//        field.isAccessible = true
//        @Suppress("UNCHECKED_CAST")
//        val cache = field.get(this) as MutableMap<String, AnalysisResponse>
//        cache[date] = data
//    }
//}

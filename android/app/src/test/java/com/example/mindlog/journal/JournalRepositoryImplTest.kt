package com.example.mindlog.features.journal.data.repository

import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest
import com.example.mindlog.features.journal.data.mapper.JournalMapper
import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Locale

class JournalRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var journalApi: JournalApi
    private lateinit var journalRepository: JournalRepositoryImpl
    private lateinit var journalMapper: JournalMapper // ✨ Mapper 인스턴스 추가

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .setLenient()
            .create()

        journalApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(JournalApi::class.java)

        // ✨ 실제 Mapper 인스턴스를 생성하여 Repository에 주입
        journalMapper = JournalMapper()
        journalRepository = JournalRepositoryImpl(journalApi, journalMapper)
    }


    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    // --- getJournals Tests ---
    @Test
    fun `getJournals - API를 호출하고 PagedResult(JournalEntry)를 반환한다`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { 
                    "items": [
                        {"id": 1, "title": "Test 1", "content": "c1", "emotions": [], "gratitude": "g1", "image_s3_keys": null, "created_at": "2025-01-01T12:00:00", "keywords": []},
                        {"id": 2, "title": "Test 2", "content": "c2", "emotions": [], "gratitude": "g2", "image_s3_keys": null, "created_at": "2025-01-02T12:00:00", "keywords": []}
                    ], 
                    "next_cursor": 3 
                }
                """.trimIndent()
            )
        )

        // When
        val result = journalRepository.getJournals(limit = 10, cursor = null)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("/journal/me?limit=10", request.path)
        assertEquals(2, result.items.size)
        // ✨ DTO가 아닌 JournalEntry 모델의 필드를 검증
        assertEquals(1, result.items[0].id)
        assertEquals("Test 1", result.items[0].title)
        assertEquals(3, result.nextCursor)
    }

    // --- getJournalById Tests ---
    @Test
    fun `getJournalById - 성공 시 API를 호출하고 JournalEntry를 반환한다`() = runTest {
        // Given
        val journalId = 123
        val dateString = "2025-01-01T12:00:00"
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { "id": $journalId, "title": "Test Journal", "content": "Content", "emotions": [], "gratitude": "Thanks", "image_s3_keys": null, "created_at": "$dateString", "keywords": [] }
                """.trimIndent()
            )
        )

        // When
        val result = journalRepository.getJournalById(journalId)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("/journal/$journalId", request.path)

        // ✨ DTO가 아닌 JournalEntry 모델의 필드를 검증
        assertEquals(journalId, result.id)
        assertEquals("Test Journal", result.title)
        assertEquals("Thanks", result.gratitude)

        val expectedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateString)
        assertEquals(expectedDate, result.createdAt)
    }

    // --- createJournal Tests ---
    @Test
    fun `createJournal - 올바른 본문을 보내고 생성된 id(Int)를 반환한다`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                { "id": 1, "title": "New Journal", "content": "New Content", "emotions": [], "created_at": "2025-01-01T12:00:00" }
                """.trimIndent()
            )
        )

        // When
        // ✨ 반환 타입이 JournalResponse -> Int로 변경됨
        val createdId = journalRepository.createJournal(
            "New Journal",
            "New Content",
            mapOf("happy" to 4),
            "Gratitude"
        )

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/journal/", request.path)
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains(""""title":"New Journal""""))
        assertTrue(requestBody.contains(""""gratitude":"Gratitude""""))
        assertTrue(requestBody.contains(""""happy":4"""))
        // ✨ 반환된 ID를 직접 검증
        assertEquals(1, createdId)
    }

    // --- extractKeywords Tests ---
    @Test
    fun `extractKeywords - 성공 시 API를 호출하고 List(Keyword)를 반환한다`() = runTest {
        // Given
        val journalId = 200
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { 
                    "data": [{"keyword": "test", "emotion": "happy", "summary": "요약", "weight": 0.9}] 
                }
                """.trimIndent()
            )
        )

        // When
        // ✨ 반환 타입이 KeywordListResponse -> List<Keyword>로 변경됨
        val result = journalRepository.extractKeywords(journalId)

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/journal/$journalId/analyze", request.path)

        assertNotNull(result)
        assertEquals(1, result.size)
        // ✨ DTO가 아닌 Keyword 모델의 필드를 검증
        assertEquals("test", result.first().keyword)
        assertEquals("happy", result.first().emotion)
    }

    // 나머지 테스트(update, delete, search, upload 등)는 Repository의 반환 타입이
    // Unit, String, Exception 등이라서 리팩토링의 영향을 받지 않으므로 기존 코드를 그대로 유지합니다.
    // 아래는 기존 테스트 코드를 그대로 유지한 부분입니다.

    @Test(expected = HttpException::class)
    fun `getJournalById - failure - throws HttpException for 404`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(404).setBody("""{ "detail": "Not Found" }""")
        )
        journalRepository.getJournalById(999)
    }

    @Test
    fun `updateJournal - calls correct endpoint with patch body`() = runTest {
        val journalId = 77
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody("\"Update Success\"")
        )
        val updateRequest = UpdateJournalRequest(
            title = "Updated Title",
            content = null,
            gratitude = "Updated Gratitude"
        )
        journalRepository.updateJournal(journalId, updateRequest)
        val request = mockWebServer.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/journal/$journalId", request.path)
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains(""""title":"Updated Title""""))
        assertTrue(!requestBody.contains(""""content""""))
    }

    @Test
    fun `deleteJournal - calls correct endpoint`() = runTest {
        val journalId = 55
        mockWebServer.enqueue(MockResponse().setResponseCode(204))
        journalRepository.deleteJournal(journalId)
        val request = mockWebServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/journal/$journalId", request.path)
    }

    @Test
    fun `searchJournals - calls correct api endpoint with title`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "items": [], "next_cursor": null }"""
            )
        )
        journalRepository.searchJournals(null, null, "검색어", 10, null)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/journal/search?"))
        assertTrue(request.path!!.contains("title=%EA%B2%80%EC%83%89%EC%96%B4"))
        assertTrue(request.path!!.contains("limit=10"))
    }

    @Test
    fun `uploadJournalImage - executes 3 steps successfully`() = runTest {
        val journalId = 101
        val s3Key = "uploads/image.jpg"
        val presignedUrl = mockWebServer.url("/s3-fake-upload")

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{ "presigned_url": "$presignedUrl", "s3_key": "$s3Key" }"""))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{ "id": 1, "journal_id": $journalId, "s3_key": "$s3Key", "created_at": "2025-01-01T12:00:00" }"""))

        journalRepository.uploadJournalImage(journalId, "test-data".toByteArray(), "image/jpeg", "image.jpg")

        val req1 = mockWebServer.takeRequest()
        assertEquals("/journal/$journalId/image", req1.path)
        val req2 = mockWebServer.takeRequest()
        assertEquals("/s3-fake-upload", req2.path)
        val req3 = mockWebServer.takeRequest()
        assertEquals("/journal/$journalId/image/complete", req3.path)
    }

    @Test(expected = RuntimeException::class)
    fun `uploadJournalImage - throws exception on S3 upload failure`() = runTest {
        val journalId = 101
        val presignedUrl = mockWebServer.url("/s3-fake-upload")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{ "presigned_url": "$presignedUrl", "s3_key": "key" }"""))
        mockWebServer.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))
        journalRepository.uploadJournalImage(journalId, "data".toByteArray(), "image/jpeg", "file.jpg")
    }

    @Test
    fun `generateImage - success - returns base64 string`() = runTest {
        val base64String = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{ "image_base64": "$base64String" }"""))
        val result = journalRepository.generateImage("style", "content")
        assertEquals(base64String, result)
    }

    @Test(expected = RuntimeException::class)
    fun `generateImage - failure - throws correct exception on timeout`() = runTest {
        val mockApi: JournalApi = mock()
        doThrow(SocketTimeoutException()).whenever(mockApi).generateImage(any())
        val repoWithMockApi = JournalRepositoryImpl(mockApi, journalMapper) // ✨ mapper 주입
        repoWithMockApi.generateImage("style", "content")
    }

    @Test(expected = RuntimeException::class)
    fun `uploadJournalImage - throws exception on presigned URL failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        journalRepository.uploadJournalImage(101, "test-data".toByteArray(), "image/jpeg", "image.jpg")
    }

    @Test(expected = RuntimeException::class)
    fun `uploadJournalImage - throws exception on complete upload failure`() = runTest {
        val journalId = 101
        val s3Key = "uploads/image.jpg"
        val presignedUrl = mockWebServer.url("/s3-fake-upload")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{ "presigned_url": "$presignedUrl", "s3_key": "$s3Key" }"""))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        journalRepository.uploadJournalImage(journalId, "test-data".toByteArray(), "image/jpeg", "image.jpg")
    }

    @Test
    fun `searchByKeyword - API를 호출하고 PagedResult(JournalEntry)를 반환한다`() = runTest {
        // Given
        val keyword = "행복"
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { 
                    "items": [
                        {
                            "id": 10, 
                            "title": "키워드 일기", 
                            "content": "내용", 
                            "emotions": [], 
                            "gratitude": "감사", 
                            "image_s3_keys": null, 
                            "created_at": "2025-01-01T10:00:00", 
                            "keywords": [{"keyword": "행복", "emotion": "happy", "summary": "s", "weight": 1.0}]
                        }
                    ], 
                    "next_cursor": 5 
                }
                """.trimIndent()
            )
        )

        // When
        val result = journalRepository.searchByKeyword(keyword, limit = 10, cursor = null)

        // Then
        val request = mockWebServer.takeRequest()

        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/journal/search-keyword"))
        assertTrue(request.path!!.contains("limit=10"))

        assertEquals(1, result.items.size)
        assertEquals(10, result.items[0].id)
        assertEquals("키워드 일기", result.items[0].title)
        assertEquals("행복", result.items[0].keywords.first().keyword)
        assertEquals(5, result.nextCursor)
    }

}

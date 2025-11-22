package com.example.mindlog.journal

import ImageUploadCompleteRequest
import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.*
import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Journal 관련 DTO들이 JSON으로부터 올바르게 파싱되는지 검증하는 테스트 클래스.
 * Repository의 로직과는 무관하게, 순수 데이터 매핑만 테스트합니다.
 */
class JournalDtoParsingTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var journalApi: JournalApi

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()

        journalApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(JournalApi::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `JournalListResponse DTO가 올바르게 파싱된다`() = runTest {
        // Given
        val json = """
            {
                "items": [
                    {
                        "id": 1, 
                        "title": "t", "content": "c", "emotions": [], "gratitude": "g", 
                        "image_s3_keys": "key", "created_at": "2025-01-01T12:00:00", "keywords": []
                    }
                ],
                "next_cursor": 2
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        // When
        val response = journalApi.getJournals(10, null) // API 호출을 통해 파싱 실행

        // Then
        assertNotNull(response)
        assertEquals(1, response.items.size)
        assertEquals(1, response.items.first().id)
        assertEquals("key", response.items.first().imageS3Keys)
        assertEquals(2, response.nextCursor)
    }

    @Test
    fun `JournalResponse DTO가 올바르게 파싱된다`() = runTest {
        // Given
        val json = """
            { 
                "id": 99, "title": "t", "content": "c", 
                "emotions": [{"emotion": "happy", "intensity": 4}], 
                "created_at": "2025-01-01T12:00:00"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        // When
        // ✨[수정] mock() 대신 실제 Request 객체 사용
        val request = JournalRequest("title", "content", emptyMap(), "gratitude")
        val response = journalApi.createJournal(request)

        // Then
        assertNotNull(response)
        assertEquals(99, response.id)
        assertEquals("t", response.title)
        assertEquals(1, response.emotions.size)
        assertEquals("happy", response.emotions.first().emotion)
    }

    @Test
    fun `KeywordListResponse DTO가 올바르게 파싱된다`() = runTest {
        // Given
        val json = """
            {
                "data": [
                    {"keyword": "k", "emotion": "e", "summary": "s", "weight": 0.5}
                ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        // When
        val response = journalApi.extractKeywords(1) // API 호출을 통해 파싱 실행

        // Then
        assertNotNull(response)
        assertEquals(1, response.data.size)
        assertEquals("k", response.data.first().keyword)
        assertEquals(0.5f, response.data.first().weight)
    }

    @Test
    fun `PresignedUrlResponse DTO가 올바르게 파싱된다`() = runTest {
        // Given
        val json = """
            {
                "presigned_url": "http://presigned",
                "file_url": "http://file",
                "s3_key": "my-key"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        // When
        // ✨[수정] mock() 대신 실제 Request 객체 사용
        val request = ImageUploadRequest("file.jpg", "image/jpeg")
        val response = journalApi.generatePresignedUrl(1, request)

        // Then
        assertNotNull(response)
        assertEquals("http://presigned", response.presignedUrl)
        assertEquals("http://file", response.fileUrl)
        assertEquals("my-key", response.s3Key)
    }

    @Test
    fun `GenerateImageResponse DTO가 올바르게 파싱된다`() = runTest {
        // Given
        val json = """{ "image_base64": "base64string" }"""
        mockWebServer.enqueue(MockResponse().setBody(json))

        // When
        // ✨[수정] mock() 대신 실제 Request 객체 사용
        val request = GenerateImageRequest("style", "content")
        val response = journalApi.generateImage(request)

        // Then
        assertNotNull(response)
        assertEquals("base64string", response.imageBase64)
    }

    @Test
    fun `ImageUploadCompleteResponse DTO가 올바르게 파싱된다`() = runTest {
        // Given
        val json = """
            {
                "id": 1,
                "journal_id": 10,
                "s3_key": "completed-key",
                "created_at": "2025-01-01T12:00:00"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(json))

        // When
        // ✨[수정] mock() 대신 실제 Request 객체 사용
        val request = ImageUploadCompleteRequest("s3-key")
        val response = journalApi.completeImageUpload(1, request)

        // Then
        assertNotNull(response)
        assertEquals(1, response.id)
        assertEquals(10, response.journalId)
        assertEquals("completed-key", response.s3Key)
        assertNotNull(response.createdAt)
    }
}

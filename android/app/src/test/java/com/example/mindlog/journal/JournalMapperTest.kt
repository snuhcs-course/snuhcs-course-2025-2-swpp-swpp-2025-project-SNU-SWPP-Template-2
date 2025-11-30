package com.example.mindlog.journal

import com.example.mindlog.BuildConfig
import com.example.mindlog.features.journal.data.dto.EmotionResponse
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.KeywordResponse
import com.example.mindlog.features.journal.data.mapper.JournalMapper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class JournalMapperTest {

    private lateinit var mapper: JournalMapper
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    @Before
    fun setup() {
        // ✨ [수정] 리플렉션을 이용한 불안정한 Mocking 코드를 제거하고, 실제 Mapper를 생성합니다.
        mapper = JournalMapper()
    }

    // 기본 DTO 객체를 생성하는 헬퍼 함수
    private fun createDefaultDto(
        id: Int = 1,
        imageS3Keys: String? = "test.jpg",
        keywords: List<KeywordResponse>? = emptyList(),
        createdAt: String = "2025-11-15T10:00:00"
    ): JournalItemResponse {
        return JournalItemResponse(
            id = id,
            title = "테스트 제목",
            content = "테스트 내용",
            createdAt = createdAt,
            emotions = listOf(EmotionResponse("happy", 4)),
            gratitude = "감사",
            imageS3Keys = imageS3Keys,
            keywords = keywords
        )
    }

    @Test
    fun `toJournalEntry - 모든 필드가 올바르게 매핑된다`() {
        // Given
        val keywordDto = KeywordResponse(keyword = "여행", emotion = "happy", summary = "즐거운 여행", weight = 0.9f)
        val dto = createDefaultDto(id = 10, imageS3Keys = "image/photo.png", keywords = listOf(keywordDto))

        // When
        val entry = mapper.toJournalEntry(dto)

        // Then
        assertEquals(10, entry.id)
        assertEquals("테스트 제목", entry.title)
        assertEquals("테스트 내용", entry.content)
        assertEquals(dateFormat.parse("2025-11-15T10:00:00"), entry.createdAt)
        assertEquals(1, entry.emotions.size)
        assertEquals("happy", entry.emotions.first().emotion)

        // Then: Image URL 검증
        assertNotNull(entry.imageUrl)
        // ✨ [수정] Mocking된 URL 대신 실제 BuildConfig 값을 사용하여 예상 URL을 만듭니다.
        assertEquals("${BuildConfig.S3_BUCKET_URL}/image/photo.png", entry.imageUrl)

        // Then: Keywords 검증
        assertEquals(1, entry.keywords.size)
        assertEquals("여행", entry.keywords.first().keyword)
        assertEquals(0.9f, entry.keywords.first().weight, 0.001f)
    }

    @Test
    fun `toJournalEntry - imageS3Keys가 null일 때 imageUrl도 null이다`() {
        // Given
        val dto = createDefaultDto(imageS3Keys = null)

        // When
        val entry = mapper.toJournalEntry(dto)

        // Then
        assertNull(entry.imageUrl)
    }

    @Test
    fun `toJournalEntry - imageS3Keys가 빈 문자열일 때 imageUrl은 null이다`() {
        // Given
        val dto = createDefaultDto(imageS3Keys = "") // blank string

        // When
        val entry = mapper.toJournalEntry(dto)

        // Then
        assertNull(entry.imageUrl)
    }

    @Test
    fun `toJournalEntry - keywords가 null일 때 빈 리스트로 매핑된다`() {
        // Given
        val dto = createDefaultDto(keywords = null)

        // When
        val entry = mapper.toJournalEntry(dto)

        // Then
        assertNotNull(entry.keywords)
        assertTrue(entry.keywords.isEmpty())
    }

    @Test
    fun `toJournalEntry - 잘못된 날짜 형식일 경우 현재 시간과 유사한 Date 객체를 반환한다`() {
        // Given
        val dto = createDefaultDto(createdAt = "invalid-date-format")

        // When
        val entry = mapper.toJournalEntry(dto)

        // Then
        // 정확한 시간을 비교하기는 어려우므로, 현재 시간과 1초 이내인지 확인
        val now = System.currentTimeMillis()
        val entryTime = entry.createdAt.time
        assertTrue("파싱 실패 시 현재 시간과 유사해야 함", (now - entryTime) < 1000)
    }

}

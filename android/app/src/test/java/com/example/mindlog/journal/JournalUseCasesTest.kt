package com.example.mindlog.journal

import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.core.model.PagedResult
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import com.example.mindlog.features.journal.domain.usecase.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Date

class JournalUseCasesTest {

    private lateinit var mockRepository: JournalRepository

    // 테스트 대상 UseCases
    private lateinit var createJournalUseCase: CreateJournalUseCase
    private lateinit var deleteJournalUseCase: DeleteJournalUseCase
    private lateinit var getJournalUseCase: GetJournalUseCase
    private lateinit var getJournalByIdUseCase: GetJournalByIdUseCase
    private lateinit var searchJournalsUseCase: SearchJournalsUseCase
    private lateinit var updateJournalUseCase: UpdateJournalUseCase
    private lateinit var extractKeywordsUseCase: ExtractKeywordsUseCase
    private lateinit var generateImageUseCase: GenerateImageUseCase
    private lateinit var uploadJournalImageUseCase: UploadJournalImageUseCase

    // 테스트에서 사용할 더미 UI 모델
    private val dummyJournalEntry = JournalEntry(
        id = 123,
        title = "제목",
        content = "내용",
        createdAt = Date(),
        imageUrl = null,
        keywords = emptyList(),
        emotions = emptyList(),
        gratitude = ""
    )

    @Before
    fun setup() {
        mockRepository = mock()

        // 각 UseCase 초기화
        createJournalUseCase = CreateJournalUseCase(mockRepository)
        deleteJournalUseCase = DeleteJournalUseCase(mockRepository)
        getJournalUseCase = GetJournalUseCase(mockRepository)
        getJournalByIdUseCase = GetJournalByIdUseCase(mockRepository)
        searchJournalsUseCase = SearchJournalsUseCase(mockRepository)
        updateJournalUseCase = UpdateJournalUseCase(mockRepository)
        extractKeywordsUseCase = ExtractKeywordsUseCase(mockRepository)
        generateImageUseCase = GenerateImageUseCase(mockRepository)
        uploadJournalImageUseCase = UploadJournalImageUseCase(mockRepository)
    }

    // --- CreateJournalUseCase Test ---
    @Test
    fun `CreateJournalUseCase - repository의 createJournal을 호출하고 생성된 ID(Int)를 반환한다`() = runTest {
        // Given
        val title = "새 일기"
        val content = "내용"
        val emotions = mapOf("happy" to 4)
        val gratitude = "감사"
        // Repository가 Int를 반환하도록 Mocking
        whenever(mockRepository.createJournal(any(), any(), any(), any())).thenReturn(1)

        // When
        val resultId = createJournalUseCase(title, content, emotions, gratitude)

        // Then
        verify(mockRepository, times(1)).createJournal(title, content, emotions, gratitude)
        assertEquals(1, resultId)
    }


    // --- UpdateJournalUseCase Tests ---
    @Test
    fun `UpdateJournalUseCase - 제목만 변경된 경우 - title만 포함된 request로 repository를 호출한다`() = runTest {
        // Given
        val journalId = 1
        val newTitle = "수정된 제목"

        // When
        updateJournalUseCase(
            journalId = journalId,
            originalTitle = "원본 제목", newTitle = newTitle,
            originalContent = "내용", newContent = "내용",
            originalGratitude = "감사", newGratitude = "감사"
        )

        // Then
        val requestCaptor = argumentCaptor<UpdateJournalRequest>()
        verify(mockRepository, times(1)).updateJournal(eq(journalId), requestCaptor.capture())

        val capturedRequest = requestCaptor.firstValue
        assertEquals(newTitle, capturedRequest.title)
        assertNull(capturedRequest.content)
        assertNull(capturedRequest.gratitude)
    }

    @Test
    fun `UpdateJournalUseCase - 아무것도 변경되지 않은 경우 - repository를 호출하지 않는다`() = runTest {
        // Given
        val journalId = 1

        // When
        updateJournalUseCase(
            journalId = journalId,
            originalTitle = "제목", newTitle = "제목",
            originalContent = "내용", newContent = "내용",
            originalGratitude = "감사", newGratitude = "감사"
        )

        // Then
        verify(mockRepository, never()).updateJournal(any(), any())
    }

    // --- DeleteJournalUseCase Test ---
    @Test
    fun `DeleteJournalUseCase - repository의 deleteJournal을 올바른 id로 호출한다`() = runTest {
        // Given
        val journalId = 123

        // When
        deleteJournalUseCase(journalId)

        // Then
        verify(mockRepository, times(1)).deleteJournal(journalId)
    }


    // --- Repository를 그대로 호출하는 UseCase들 테스트 ---
    @Test
    fun `GetJournalUseCase - repository의 getJournals를 호출하고 PagedResult를 반환한다`() = runTest {
        // Given
        val dummyPagedResult = PagedResult(items = listOf(dummyJournalEntry), nextCursor = 2)
        whenever(mockRepository.getJournals(any(), anyOrNull())).thenReturn(dummyPagedResult)

        // When
        val result = getJournalUseCase(limit = 10, cursor = null)

        // Then
        verify(mockRepository, times(1)).getJournals(10, null)
        assertEquals(dummyPagedResult, result)
        assertEquals(123, result.items.first().id)
    }

    @Test
    fun `SearchJournalsUseCase - repository의 searchJournals를 호출하고 PagedResult를 반환한다`() = runTest {
        // Given
        val dummyPagedResult = PagedResult(items = emptyList<JournalEntry>(), nextCursor = null)
        whenever(mockRepository.searchJournals(anyOrNull(), anyOrNull(), anyOrNull(), any(), anyOrNull())).thenReturn(dummyPagedResult)
        // When
        searchJournalsUseCase(startDate = "2025-01-01", endDate = null, title = "검색", limit = 5, cursor = null)

        // Then
        verify(mockRepository, times(1)).searchJournals("2025-01-01", null, "검색", 5, null)
    }

    @Test
    fun `GetJournalByIdUseCase - repository의 getJournalById를 호출하고 JournalEntry를 반환한다`() = runTest {
        // Given
        whenever(mockRepository.getJournalById(any())).thenReturn(dummyJournalEntry)

        // When
        val result = getJournalByIdUseCase(journalId = 123)

        // Then
        verify(mockRepository, times(1)).getJournalById(123)
        assertEquals(dummyJournalEntry, result)
    }

    @Test
    fun `ExtractKeywordsUseCase - repository의 extractKeywords를 호출하고 List-Keyword-를 반환한다`() = runTest {
        // Given
        val dummyKeywords = listOf(Keyword("테스트", "happy", "요약", 0.9f))
        whenever(mockRepository.extractKeywords(any())).thenReturn(dummyKeywords)

        // When
        val result = extractKeywordsUseCase(1)

        // Then
        verify(mockRepository, times(1)).extractKeywords(1)
        assertEquals(dummyKeywords, result)
    }


    @Test
    fun `GenerateImageUseCase - repository의 generateImage를 호출한다`() = runTest {
        // When
        generateImageUseCase("style", "content")

        // Then
        verify(mockRepository, times(1)).generateImage("style", "content")
    }

    @Test
    fun `UploadJournalImageUseCase - repository의 uploadJournalImage를 호출한다`() = runTest {
        // Given
        val imageBytes = "data".toByteArray()

        // When
        uploadJournalImageUseCase(1, imageBytes, "image/jpeg", "file.jpg")

        // Then
        verify(mockRepository, times(1)).uploadJournalImage(1, imageBytes, "image/jpeg", "file.jpg")
    }

    @Test
    fun `SearchByKeywordUseCase - repository의 searchByKeyword를 호출하고 PagedResult를 반환한다`() = runTest {
        // Given
        val keyword = "여행"
        val limit = 10
        val cursor = null
        val dummyPagedResult = PagedResult(items = listOf(dummyJournalEntry), nextCursor = 2)

        val searchByKeywordUseCase = SearchByKeywordUseCase(mockRepository)
        whenever(mockRepository.searchByKeyword(any(), any(), anyOrNull())).thenReturn(dummyPagedResult)

        // When
        val result = searchByKeywordUseCase(keyword, limit, cursor)

        // Then
        verify(mockRepository, times(1)).searchByKeyword(keyword, limit, cursor)
        assertEquals(dummyPagedResult, result)
        assertEquals(123, result.items.first().id)
    }

}

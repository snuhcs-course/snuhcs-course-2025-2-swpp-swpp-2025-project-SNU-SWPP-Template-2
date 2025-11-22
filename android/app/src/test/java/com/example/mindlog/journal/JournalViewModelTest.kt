package com.example.mindlog.journal

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.PagedResult
import com.example.mindlog.features.journal.domain.usecase.GetJournalByIdUseCase
import com.example.mindlog.features.journal.domain.usecase.GetJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.SearchJournalsUseCase
import com.example.mindlog.features.journal.presentation.list.JournalViewModel
import com.example.mindlog.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var getJournalsUseCase: GetJournalUseCase
    private lateinit var searchJournalsUseCase: SearchJournalsUseCase
    private lateinit var getJournalByIdUseCase: GetJournalByIdUseCase

    private lateinit var viewModel: JournalViewModel

    // ✨ 테스트에서 사용할 더미 UI 모델 객체 생성 함수
    private fun createDummyJournalEntry(id: Int, title: String = "제목 $id") = JournalEntry(
        id = id,
        title = title,
        content = "내용 $id",
        createdAt = Date(),
        imageUrl = null,
        keywords = emptyList(),
        emotions = emptyList(),
        gratitude = "감사 $id"
    )

    @Before
    fun setup() {
        getJournalsUseCase = mock()
        searchJournalsUseCase = mock()
        getJournalByIdUseCase = mock()

        viewModel = JournalViewModel(
            getJournalsUseCase,
            searchJournalsUseCase,
            getJournalByIdUseCase
        )
    }

    @Test
    fun `loadJournals 성공 시 - journals LiveData를 업데이트하고 로딩을 false로 설정한다`() = runTest {
        // Given
        // ✨ DTO 대신 PagedResult<JournalEntry>를 반환하도록 설정
        val dummyResult = PagedResult(
            items = listOf(createDummyJournalEntry(1), createDummyJournalEntry(2)),
            nextCursor = 3
        )
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenReturn(dummyResult)

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
        assertEquals(2, viewModel.journals.value?.size)
        assertEquals(1, viewModel.journals.value?.first()?.id)
        assertEquals(false, viewModel.isLastPage)
    }

    @Test
    fun `loadJournals 실패 시 - 에러 메시지를 설정하고 로딩을 false로 설정한다`() = runTest {
        // Given
        val errorMessage = "네트워크 오류 발생"
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenThrow(RuntimeException(errorMessage))

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.isLoading.value)
        assertTrue(viewModel.errorMessage.value?.contains(errorMessage) ?: false)
        assertTrue(viewModel.journals.value?.isEmpty() ?: true)
    }

    @Test
    fun `loadMoreJournals - 기존 목록에 새로운 아이템들을 추가한다`() = runTest {
        // Given: 첫 페이지 로드
        val firstResult = PagedResult(items = listOf(createDummyJournalEntry(1)), nextCursor = 2)
        whenever(getJournalsUseCase.invoke(limit = 10, cursor = null)).thenReturn(firstResult)
        viewModel.loadJournals()
        advanceUntilIdle()
        assertEquals(1, viewModel.journals.value?.size)

        // Given: 두 번째 페이지 응답 설정
        val secondResult = PagedResult(items = listOf(createDummyJournalEntry(2)), nextCursor = null)
        whenever(getJournalsUseCase.invoke(limit = 10, cursor = 2)).thenReturn(secondResult)

        // When: 추가 로드
        viewModel.loadMoreJournals()
        advanceUntilIdle()

        // Then
        assertEquals(2, viewModel.journals.value?.size)
        assertEquals(2, viewModel.journals.value?.get(1)?.id)
        assertEquals(true, viewModel.isLastPage)
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = 2)
    }

    @Test
    fun `검색어가 있는 상태에서 loadJournals 호출 시 - searchJournalsUseCase를 호출한다`() = runTest {
        // Given
        val query = "검색어"
        viewModel.searchQuery.value = query
        val dummyResult = PagedResult(items = listOf(createDummyJournalEntry(10)), nextCursor = null)
        whenever(searchJournalsUseCase.invoke(any(), any(), any(), any(), anyOrNull())).thenReturn(dummyResult)

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        verify(searchJournalsUseCase, times(1)).invoke(
            startDate = null,
            endDate = null,
            title = query,
            limit = 10,
            cursor = null
        )
        verify(getJournalsUseCase, never()).invoke(any(), anyOrNull())
    }

    @Test
    fun `updateOrRemoveJournalEntry - deletedId가 있으면 목록에서 아이템을 제거한다`() = runTest {
        // Given
        val initialList = listOf(createDummyJournalEntry(1), createDummyJournalEntry(2))
        viewModel.journals.value = initialList
        assertEquals(2, viewModel.journals.value?.size)

        // When
        viewModel.updateOrRemoveJournalEntry(updatedId = null, deletedId = 2)
        advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.journals.value?.size)
        assertNull(viewModel.journals.value?.find { it.id == 2 })
    }

    @Test
    fun `updateOrRemoveJournalEntry - updatedId가 있으면 목록의 아이템을 갱신한다`() = runTest {
        // Given
        val initialList = listOf(createDummyJournalEntry(1, "원본 제목"))
        viewModel.journals.value = initialList
        // ✨ DTO가 아닌 JournalEntry를 반환하도록 설정
        val updatedItem = createDummyJournalEntry(1, "수정된 제목")
        whenever(getJournalByIdUseCase.invoke(1)).thenReturn(updatedItem)

        // When
        viewModel.updateOrRemoveJournalEntry(updatedId = 1, deletedId = null)
        advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.journals.value?.size)
        assertEquals("수정된 제목", viewModel.journals.value?.first()?.title)
        verify(getJournalByIdUseCase, times(1)).invoke(1)
    }

    // ✨ [복원된 테스트] 나머지 테스트들도 DTO 대신 UI 모델을 사용하도록 수정
    @Test
    fun `setDateRange - 상태만 업데이트하고 UseCase는 호출하지 않는다`() = runTest {
        val startDate = "2025-01-01"
        val endDate = "2025-01-31"

        viewModel.setDateRange(startDate, endDate)
        advanceUntilIdle()

        assertEquals(startDate, viewModel.startDate.value)
        assertEquals(endDate, viewModel.endDate.value)

        verifyNoInteractions(searchJournalsUseCase)
        verifyNoInteractions(getJournalsUseCase)
    }

    @Test
    fun `날짜 범위가 있는 상태에서 loadJournals - searchJournalsUseCase를 올바른 날짜로 호출한다`() = runTest {
        val startDate = "2025-01-15"
        val endDate = "2025-01-20"
        viewModel.setDateRange(startDate, endDate)

        whenever(searchJournalsUseCase.invoke(any(), any(), any(), any(), anyOrNull()))
            .thenReturn(PagedResult(emptyList(), null))

        viewModel.loadJournals()
        advanceUntilIdle()

        verify(searchJournalsUseCase, times(1)).invoke(
            startDate = startDate,
            endDate = endDate,
            title = null,
            limit = 10,
            cursor = null
        )
        verify(getJournalsUseCase, never()).invoke(any(), anyOrNull())
    }

    @Test
    fun `clearSearchAndReload - 모든 검색 상태를 초기화하고 목록을 새로고침한다`() = runTest {
        viewModel.searchQuery.value = "기존 검색어"
        viewModel.startDate.value = "2025-01-01"
        whenever(getJournalsUseCase.invoke(any(), anyOrNull()))
            .thenReturn(PagedResult(emptyList(), null))

        viewModel.clearSearchAndReload()
        advanceUntilIdle()

        assertNull(viewModel.searchQuery.value)
        assertNull(viewModel.startDate.value)
        assertNull(viewModel.endDate.value)
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)
        verify(searchJournalsUseCase, never()).invoke(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `startObservingSearchQuery - debounce 후 loadJournals를 호출한다`() = runTest {
        whenever(searchJournalsUseCase.invoke(any(), any(), any(), any(), anyOrNull()))
            .thenReturn(PagedResult(emptyList(), null))

        viewModel.startObservingSearchQuery()
        viewModel.searchQuery.value = "새로운 검색어"
        advanceUntilIdle()

        verify(searchJournalsUseCase, times(1)).invoke(
            startDate = null,
            endDate = null,
            title = "새로운 검색어",
            limit = 10,
            cursor = null
        )
    }

    @Test
    fun `loadJournals - 로딩 중일 때는 다시 호출되지 않는다`() = runTest {
        viewModel.isLoading.value = true

        viewModel.loadJournals()
        advanceUntilIdle()

        verify(getJournalsUseCase, never()).invoke(any(), anyOrNull())
        verify(searchJournalsUseCase, never()).invoke(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `updateOrRemoveJournalEntry - 존재하지 않는 deletedId는 무시한다`() = runTest {
        val initialList = listOf(createDummyJournalEntry(1))
        viewModel.journals.value = initialList

        viewModel.updateOrRemoveJournalEntry(updatedId = null, deletedId = 99)
        advanceUntilIdle()

        assertEquals(1, viewModel.journals.value?.size)
    }

    @Test
    fun `updateOrRemoveJournalEntry - 존재하지 않는 updatedId는 무시한다`() = runTest {
        val initialList = listOf(createDummyJournalEntry(1))
        viewModel.journals.value = initialList

        viewModel.updateOrRemoveJournalEntry(updatedId = 99, deletedId = null)
        advanceUntilIdle()

        verify(getJournalByIdUseCase, never()).invoke(any())
        assertEquals(1, viewModel.journals.value?.size)
    }

    @Test
    fun `loadMoreJournals 실패 시 - 에러 메시지를 설정한다`() = runTest {
        val firstResult = PagedResult(items = listOf(createDummyJournalEntry(1)), nextCursor = 2)
        whenever(getJournalsUseCase.invoke(limit = 10, cursor = null)).thenReturn(firstResult)
        viewModel.loadJournals()
        advanceUntilIdle()

        val errorMessage = "더보기 로딩 실패"
        whenever(getJournalsUseCase.invoke(limit = 10, cursor = 2)).thenThrow(RuntimeException(errorMessage))

        viewModel.loadMoreJournals()
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assertTrue(viewModel.errorMessage.value?.contains(errorMessage) ?: false)
    }

    @Test
    fun `updateOrRemoveJournalEntry에서 아이템 갱신 실패 시 - 전체 목록을 새로고침한다`() = runTest {
        val initialList = listOf(createDummyJournalEntry(1))
        viewModel.journals.value = initialList

        whenever(getJournalByIdUseCase.invoke(1)).thenThrow(RuntimeException("상세 정보 로드 실패"))
        whenever(getJournalsUseCase.invoke(any(), anyOrNull())).thenReturn(PagedResult(emptyList(), null))

        viewModel.updateOrRemoveJournalEntry(updatedId = 1, deletedId = null)
        advanceUntilIdle()

        verify(getJournalByIdUseCase, times(1)).invoke(1)
        verify(getJournalsUseCase, times(1)).invoke(limit = 10, cursor = null)
    }

    @Test
    fun `clearSearchConditions - 상태만 초기화하고 API를 호출하지 않는다`() = runTest {
        viewModel.searchQuery.value = "검색어"
        viewModel.startDate.value = "2025-01-01"

        viewModel.clearSearchConditions()
        advanceUntilIdle()

        assertNull(viewModel.searchQuery.value)
        assertNull(viewModel.startDate.value)
        verify(getJournalsUseCase, never()).invoke(any(), anyOrNull())
        verify(searchJournalsUseCase, never()).invoke(any(), any(), any(), any(), anyOrNull())
    }
}

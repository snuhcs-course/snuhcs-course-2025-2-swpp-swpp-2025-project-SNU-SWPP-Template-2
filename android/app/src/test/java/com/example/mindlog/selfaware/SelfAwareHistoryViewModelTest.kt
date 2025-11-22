package com.example.mindlog.selfaware

import app.cash.turbine.test
import com.example.mindlog.core.common.Paged
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.selfaware.domain.model.Answer
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.model.Question
import com.example.mindlog.features.selfaware.domain.usecase.GetQAHistoryUseCase
import com.example.mindlog.features.selfaware.presentation.viewmodel.SelfAwareHistoryViewModel
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelfAwareHistoryViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private lateinit var getQAHistoryUseCase: GetQAHistoryUseCase
    private lateinit var vm: SelfAwareHistoryViewModel

    @Before fun setup() {
        val testDispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)

        getQAHistoryUseCase = mock()
        vm = SelfAwareHistoryViewModel(getQAHistoryUseCase, testDispatcherProvider)
    }

    @Test
    fun `refresh success updates items and nextCursor`() = runTest {
        val page = Paged(items = listOf(testItem(1), testItem(2)), cursor = 2, size = 2)
        whenever(getQAHistoryUseCase(limit = any(), cursor = anyOrNull()))
            .thenReturn(Result.Success(page))

        // when
        vm.state.test {
            val initial = awaitItem()
            println(initial.items)
            assertTrue(initial.items.isEmpty())

            vm.refresh()
            val loading = awaitItem()
            assertTrue(loading.isRefreshing) // 구현에 따라 둘 중 하나

            val done = awaitItem()
            assertFalse(done.isRefreshing)
            assertEquals(2, done.items.size)
            assertEquals(2, done.nextCursor)
            assertFalse(done.isEnd)

            cancelAndConsumeRemainingEvents()
        }

        verify(getQAHistoryUseCase, times(1)).invoke(limit = any(), cursor = anyOrNull())
        verifyNoMoreInteractions(getQAHistoryUseCase)
    }

    @Test
    fun `refresh error sets error and stops loading`() = runTest {
        whenever(getQAHistoryUseCase(limit = any(), cursor = anyOrNull()))
            .thenReturn(Result.Error(500, "network error"))

        vm.state.test {
            awaitItem()
            vm.refresh()

            val loading = awaitItem()
            assertTrue(loading.isRefreshing)

            val errored = awaitItem()
            assertFalse(errored.isRefreshing)
            assertNotNull(errored.error)
            assertTrue(errored.items.isEmpty())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loadNext success appends items and advances cursor`() = runTest {
        // 첫 페이지
        val page1 = Paged(items = (1..10).map { testItem(it) }, cursor = 11, size = 10)
        whenever(getQAHistoryUseCase(limit = any(), cursor = anyOrNull()))
            .thenReturn(Result.Success(page1))

        vm.refresh()
        advanceUntilIdle()

        // 다음 페이지
        val page2 = Paged(items = listOf(testItem(11)), cursor = null, size = 1)
        whenever(getQAHistoryUseCase(limit = any(), cursor = anyOrNull()))
            .thenReturn(Result.Success(page2))

        vm.state.test {
            // 현재 상태 소비
            skipItems(1) // 현재 state 1개 스킵

            vm.loadNext()

            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val done = awaitItem()
            assertFalse(done.isLoading)
            assertEquals(11, done.items.size) // 10 + 1
            assertNull(done.nextCursor)
            assertTrue(done.isEnd)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loadNext no-op when already loading or end`() = runTest {
        // 첫 페이지
        val page = Paged(items = listOf(testItem(1)), cursor = null, size = 1)
        whenever(getQAHistoryUseCase(limit = any(), cursor = anyOrNull()))
            .thenReturn(Result.Success(page))

        vm.refresh()
        advanceUntilIdle()

        // 이미 isEnd = true 상태에서 loadNext 호출 → 호출되지 않아야 함
        vm.loadNext()
        verify(getQAHistoryUseCase, times(1)).invoke(limit = any(), cursor = anyOrNull())
        verifyNoMoreInteractions(getQAHistoryUseCase)
    }

    private fun testItem(id: Int) = QAItem(
        question = Question(
            id = id,
            type = "Question Type",
            text = "Q$id",
            createdAt = LocalDate.now(),
        ),
        answer = Answer(
            id = id,
            questionId = id,
            type = "",
            text = "A$id",
            createdAt = LocalDate.now(),
            updatedAt = LocalDate.now()
        )
    )
}
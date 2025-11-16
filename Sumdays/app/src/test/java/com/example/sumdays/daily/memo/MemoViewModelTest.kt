package com.example.sumdays.daily.memo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MemoViewModelTest {
    // ⭐ 핵심 해결책: LiveData의 메인 스레드 제약을 우회하는 Rule 추가
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // 1. Mock 객체 선언
    private val mockRepository: MemoRepository = mock()
    private lateinit var viewModel: MemoViewModel

    // 2. Coroutine Dispatcher 설정 (비동기 테스트를 동기적으로 실행하기 위해 필수)
    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setup() {
        // Coroutine 테스트 환경 설정
        Dispatchers.setMain(testDispatcher)

        // Mock Repository를 주입하여 ViewModel 인스턴스 생성
        viewModel = MemoViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        // Coroutine 테스트 환경 정리
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    // =================================================================
    // 1. 데이터 조회 테스트 (getMemosForDate)
    // =================================================================
    @Test
    fun getMemosForDate_callsRepositoryAndReturnsLiveData() {
        // Arrange
        val testDate = "2025-10-31"
        val dummyMemo = Memo(id=1, content="Test", timestamp="10:00", date=testDate, order=0)
        // Repository가 flowOf()를 반환하도록 Mocking
        whenever(mockRepository.getMemosForDate(testDate)).thenReturn(flowOf(listOf(dummyMemo)))

        // Act
        val liveData = viewModel.getMemosForDate(testDate)

        // Assert
        // Repository 함수가 올바른 인자로 호출되었는지 검증
        verify(mockRepository).getMemosForDate(testDate)

        // LiveData가 Null이 아닌지, Observer를 붙여 값이 발행되는지 검증
        var result: List<Memo>? = null
        val observer = Observer<List<Memo>> { result = it }
        liveData.observeForever(observer)

        testDispatcher.scheduler.advanceUntilIdle() // 비동기 작업 완료 대기

        Assert.assertNotNull(result)
        Assert.assertEquals(1, result?.size)

        liveData.removeObserver(observer)
    }

    // =================================================================
    // 2. 메모 추가/삽입 테스트 (insert)
    // =================================================================
    @Test
    fun insert_callsRepositoryInsert() = testDispatcher.runBlockingTest {
        // Arrange
        val newMemo = Memo(content="New memo content", date="2025-11-01", timestamp="12:00", order=0)

        // Act
        viewModel.insert(newMemo)

        // Assert
        // Repository의 insert 함수가 정확한 Memo 객체로 호출되었는지 검증
        verify(mockRepository).insert(newMemo)
    }

    // =================================================================
    // 3. 메모 수정 테스트 (update)
    // =================================================================
    @Test
    fun update_callsRepositoryUpdate() = testDispatcher.runBlockingTest {
        // Arrange
        val updatedMemo = Memo(id=5, content="Updated content", date="2025-10-31", timestamp="15:00", order=1)

        // Act
        viewModel.update(updatedMemo)

        // Assert
        // Repository의 update 함수가 정확한 Memo 객체로 호출되었는지 검증
        verify(mockRepository).update(updatedMemo)
    }

    // =================================================================
    // 4. 순서 업데이트 테스트 (updateAll)
    // =================================================================
    @Test
    fun updateAll_callsRepositoryUpdateAll() = testDispatcher.runBlockingTest {
        // Arrange
        val memoList = listOf(
            Memo(id=1, content="A", date="2025-10-31", timestamp="10:00", order=1),
            Memo(id=2, content="B", date="2025-10-31", timestamp="10:01", order=0)
        )

        // Act
        viewModel.updateAll(memoList)

        // Assert
        // Repository의 updateAll 함수가 정확한 리스트로 호출되었는지 검증
        verify(mockRepository).updateAll(memoList)
    }

    // =================================================================
    // 5. 메모 삭제 테스트 (delete)
    // =================================================================
    @Test
    fun delete_callsRepositoryDelete() = testDispatcher.runBlockingTest {
        // Arrange
        val memoToDelete = Memo(id=3, content="To delete", date="2025-10-31", timestamp="11:00", order=2)

        // Act
        viewModel.delete(memoToDelete)

        // Assert
        // Repository의 delete 함수가 정확한 Memo 객체로 호출되었는지 검증
        verify(mockRepository).delete(memoToDelete)
    }
}
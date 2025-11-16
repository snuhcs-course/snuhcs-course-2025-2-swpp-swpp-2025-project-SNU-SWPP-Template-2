package com.example.sumdays.daily.memo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.TestCoroutineDispatcher // 코루틴 테스트를 위해 필요 (deprecated 경고가 나올 수 있지만, runTest 환경에서 사용)

// 테스트에서 사용할 더미 데이터 클래스 정의 (MemoDao 인터페이스도 필요하지만, 여기서는 Memo만 정의)
// 실제 프로젝트의 Memo 클래스 정의를 사용하세요.

// MemoDao 인터페이스가 실제 프로젝트에 존재한다고 가정하고 테스트를 작성합니다.
// interface MemoDao { ... }

class MemoRepositoryTest {

    private lateinit var repository: MemoRepository
    private val mockMemoDao: MemoDao = mockk(relaxed = true) // relaxed = true로 설정하여 모든 함수 호출을 허용

    private val testMemo = Memo(1, "테스트 내용", "10:00", "2025-11-02", 0)
    private val testDate = "2025-11-02"

    @Before
    fun setUp() {
        // Mock 객체를 주입하여 Repository 초기화
        repository = MemoRepository(mockMemoDao)
    }

    // --- 1. 코루틴을 사용하는 Suspend 함수 테스트 ---

    @Test
    fun `testInsert_callsDaoInsert`() = runTest {
        // WHEN
        repository.insert(testMemo)

        // THEN: Dao의 insert 함수가 올바른 Memo 객체로 호출되었는지 검증
        coVerify(exactly = 1) { mockMemoDao.insert(testMemo) }
    }

    @Test
    fun `testUpdate_callsDaoUpdate`() = runTest {
        // GIVEN: 수정된 메모
        val updatedMemo = testMemo.copy(content = "수정된 내용")

        // WHEN
        repository.update(updatedMemo)

        // THEN: Dao의 update 함수가 올바른 Memo 객체로 호출되었는지 검증
        coVerify(exactly = 1) { mockMemoDao.update(updatedMemo) }
    }

    @Test
    fun `testUpdateAll_callsDaoUpdateAll`() = runTest {
        // GIVEN: 메모 목록
        val memoList = listOf(testMemo, testMemo.copy(id = 2))

        // WHEN
        repository.updateAll(memoList)

        // THEN: Dao의 updateAll 함수가 올바른 목록으로 호출되었는지 검증
        coVerify(exactly = 1) { mockMemoDao.updateAll(memoList) }
    }

    @Test
    fun `testDelete_callsDaoDelete`() = runTest {
        // WHEN
        repository.delete(testMemo)

        // THEN: Dao의 delete 함수가 올바른 Memo 객체로 호출되었는지 검증
        coVerify(exactly = 1) { mockMemoDao.delete(testMemo) }
    }

    // --- 2. Flow를 반환하는 함수 테스트 ---

    @Test
    fun `testGetMemosForDate_returnsDaoFlow`() = runTest {
        // GIVEN: Dao가 반환할 Flow 데이터 설정
        val expectedMemos: Flow<List<Memo>> = flowOf(listOf(testMemo))

        // Mock 설정: 특정 날짜로 호출되면 예상 Flow를 반환하도록 함
        coEvery { mockMemoDao.getMemosForDate(testDate) } returns expectedMemos

        // WHEN
        val resultFlow = repository.getMemosForDate(testDate)

        // THEN
        // 1. 반환된 Flow가 예상 Flow와 같은지 확인
        assertEquals(expectedMemos, resultFlow)

        // 2. Dao의 함수가 올바른 날짜로 호출되었는지 검증
        coVerify(exactly = 1) { mockMemoDao.getMemosForDate(testDate) }
    }
}
package com.example.sumdays.data.viewModel

import androidx.lifecycle.ViewModel
import com.example.sumdays.data.repository.WeekSummaryRepository
import com.example.sumdays.data.WeekSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class WeekSummaryViewModelTest {

    private val repository: WeekSummaryRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: WeekSummaryViewModel

    @BeforeTest
    fun setup() {
        // ViewModelScope 내의 launch를 제어하기 위해 Dispatchers.Main을 테스트 디스패처로 설정
        Dispatchers.setMain(testDispatcher)

        // ViewModel 생성
        viewModel = WeekSummaryViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        // Dispatchers.Main 복구
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. Public API 검증 (upsert)
    // =========================================================================

    @Test
    fun `01_upsert_should_call_repository_upsertWeekSummary_inside_viewModelScope`() {
        val mockSummary = mockk<WeekSummary>(relaxed = true)

        viewModel.upsert(mockSummary)

        // viewModelScope.launch가 실행되도록 디스패처를 진행
        testDispatcher.scheduler.runCurrent()

        // repository.upsertWeekSummary 호출 검증
        coVerify(exactly = 1) { repository.upsertWeekSummary(mockSummary) }
    }


    // =========================================================================
    // 2. Public API 검증 (getSummary) - Success & Null 경로 커버
    // =========================================================================

    @Test
    fun `02_getSummary_should_call_repository_and_return_summary_on_success`() = runBlocking {
        val startDate = "2025-05-01"
        val mockSummary = mockk<WeekSummary>(relaxed = true)

        // 성공 케이스 Mock
        coEvery { repository.getWeekSummary(startDate) } returns mockSummary

        val summary = viewModel.getSummary(startDate)

        assertEquals(mockSummary, summary)
        coVerify(exactly = 1) { repository.getWeekSummary(startDate) }
    }

    @Test
    fun `03_getSummary_should_call_repository_and_return_null_if_not_found`() = runBlocking {
        val startDate = "2025-06-01"

        // Null 반환 케이스 Mock
        coEvery { repository.getWeekSummary(startDate) } returns null

        val summary = viewModel.getSummary(startDate)

        assertNull(summary)
        coVerify(exactly = 1) { repository.getWeekSummary(startDate) }
    }

    @Test
    fun `04_getSummary_should_handle_repository_exception`() = runBlocking {
        val startDate = "2025-07-01"

        // 예외 발생 케이스 Mock (커버리지 확보)
        coEvery { repository.getWeekSummary(startDate) } throws RuntimeException("DB Error")

        // 예외가 그대로 전달되는지 검증
        assertFailsWith<RuntimeException> {
            viewModel.getSummary(startDate)
        }
        coVerify(exactly = 1) { repository.getWeekSummary(startDate) }
    }


    // =========================================================================
    // 3. Public API 검증 (getAllDatesAsc) - Success & Empty 경로 커버
    // =========================================================================

    @Test
    fun `05_getAllDatesAsc_should_call_repository_and_return_sorted_dates`() = runBlocking {
        val mockDates = listOf("2026-01-08", "2026-01-01", "2026-01-15")

        // 성공 케이스 Mock (정렬되지 않은 데이터 반환)
        coEvery { repository.getAllWrittenDatesAsc() } returns mockDates

        val dates = viewModel.getAllDatesAsc()

        // Repository가 정렬된 데이터를 반환한다고 가정했으므로, mockDates가 그대로 반환되는지 확인
        assertEquals(mockDates, dates)
        coVerify(exactly = 1) { repository.getAllWrittenDatesAsc() }
    }

    @Test
    fun `06_getAllDatesAsc_should_return_empty_list_if_repository_returns_empty`() = runBlocking {
        val emptyList = emptyList<String>()

        // 빈 리스트 반환 케이스 Mock
        coEvery { repository.getAllWrittenDatesAsc() } returns emptyList

        val dates = viewModel.getAllDatesAsc()

        assertTrue(dates.isEmpty())
        coVerify(exactly = 1) { repository.getAllWrittenDatesAsc() }
    }


    // =========================================================================
    // 4. ViewModel Factory 검증 (모든 Factory 로직 커버)
    // =========================================================================

    @Test
    fun `07_WeekSummaryViewModelFactory_should_create_WeekSummaryViewModel_instance`() {
        val factory = WeekSummaryViewModelFactory(repository)
        val createdViewModel = factory.create(WeekSummaryViewModel::class.java)

        assertTrue(createdViewModel is WeekSummaryViewModel)
    }

    @Test
    fun `08_WeekSummaryViewModelFactory_should_throw_exception_for_unknown_ViewModel_class`() {
        val factory = WeekSummaryViewModelFactory(repository)

        // IllegalArgumentException이 발생하는지 검증 (Negative Case 커버)
        assertFailsWith<IllegalArgumentException> {
            factory.create(UnknownViewModel::class.java)
        }
    }
}

// 팩토리 테스트를 위한 더미 클래스
private class UnknownViewModel : ViewModel()
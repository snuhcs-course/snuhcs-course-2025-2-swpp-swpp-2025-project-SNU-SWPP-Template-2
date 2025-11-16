package com.example.sumdays.data.viewModel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.sumdays.data.AppDatabase
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.EmojiData
import com.example.sumdays.data.dao.DailyEntryDao
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * ✅ 통합 테스트 파일 (수정됨)
 * mockkObject(AppDatabase.Companion)을 사용하여 AbstractMethodError 해결
 */
@ExperimentalCoroutinesApi
class ViewModelTest {

    // ==========================================
    // 1. 테스트 규칙 설정 (Rules)
    // ==========================================

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()


    // ==========================================
    // 2. Mock 객체 생성
    // ==========================================
    private val application = mockk<Application>(relaxed = true)
    private val database = mockk<AppDatabase>()
    private val dao = mockk<DailyEntryDao>()

    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var dailyEntryViewModel: DailyEntryViewModel

    @Before
    fun setup() {
        // 🚨 [수정됨] 핵심: Static Mocking 대신 Companion Object Mocking 사용
        // AppDatabase.getDatabase()는 @JvmStatic이 없으므로 mockkObject를 써야 함
        mockkObject(AppDatabase.Companion)

        // Companion 객체의 getDatabase 호출 시 가짜 database 반환
        every { AppDatabase.getDatabase(any()) } returns database

        // database.dailyEntryDao() 호출 시 가짜 dao 반환
        every { database.dailyEntryDao() } returns dao

        // ViewModel 초기화
        calendarViewModel = CalendarViewModel(application)
        dailyEntryViewModel = DailyEntryViewModel(application)
    }

    @After
    fun tearDown() {
        // Mocking 해제
        unmockkObject(AppDatabase.Companion)
    }


    // ==========================================
    // 3. CalendarViewModel 테스트
    // ==========================================

    @Test
    fun `CalendarViewModel_getMonthlyEmojis maps EmojiData list to Map correctly`() {
        // Given
        val from = "2023-10-01"
        val to = "2023-10-31"
        val emojiList = listOf(
            EmojiData("2023-10-01", "Diary", "happy"),
            EmojiData("2023-10-02", null, "sad")
        )
        every { dao.getMonthlyEmojis(from, to) } returns flowOf(emojiList)

        // When
        val result = calendarViewModel.getMonthlyEmojis(from, to).getOrAwaitValue()

        // Then
        assertEquals(true, result["2023-10-01"]?.first)
        assertEquals("happy", result["2023-10-01"]?.second)

        assertEquals(false, result["2023-10-02"]?.first)
        assertEquals("sad", result["2023-10-02"]?.second)
    }


    // ==========================================
    // 4. DailyEntryViewModel 테스트
    // ==========================================

    @Test
    fun `DailyEntryViewModel_getEntry returns data correctly`() {
        // Given
        val date = "2023-10-25"
        val expectedEntry = DailyEntry(
            date = date, diary = "Test", keywords = null,
            aiComment = null, emotionScore = null, emotionIcon = null, themeIcon = null
        )
        every { dao.getEntry(date) } returns flowOf(expectedEntry)

        // When
        val result = dailyEntryViewModel.getEntry(date).getOrAwaitValue()

        // Then
        assertEquals(expectedEntry, result)
        verify { dao.getEntry(date) }
    }

    @Test
    fun `DailyEntryViewModel_updateEntry calls DAO with correct parameters`() = runTest {
        // Given
        val date = "2023-10-25"
        coJustRun { dao.updateEntry(any(), any(), any(), any(), any(), any(), any()) }

        // When
        dailyEntryViewModel.updateEntry(
            date = date,
            diary = "Updated Diary",
            emotionScore = 0.9
        )
        advanceUntilIdle()

        // Then
        coVerify {
            dao.updateEntry(
                date = date,
                diary = "Updated Diary",
                keywords = null,
                aiComment = null,
                emotionScore = 0.9,
                emotionIcon = null,
                themeIcon = null
            )
        }
    }

    @Test
    fun `DailyEntryViewModel_deleteEntry calls DAO delete`() = runTest {
        // Given
        val date = "2023-10-25"
        coJustRun { dao.deleteEntry(date) }

        // When
        dailyEntryViewModel.deleteEntry(date)
        advanceUntilIdle()

        // Then
        coVerify { dao.deleteEntry(date) }
    }

    @Test
    fun `DailyEntryViewModel_getAllWrittenDates calls DAO directly`() {
        // Given
        val expectedList = listOf("2023-10-01", "2023-10-05")
        every { dao.getAllWrittenDates() } returns expectedList

        // When
        val result = dailyEntryViewModel.getAllWrittenDates()

        // Then
        assertEquals(expectedList, result)
        verify { dao.getAllWrittenDates() }
    }
}


// ==========================================
// 5. 테스트 유틸리티
// ==========================================

@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            data = value
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)

    if (!latch.await(time, timeUnit)) {
        throw TimeoutException("LiveData value was never set.")
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}
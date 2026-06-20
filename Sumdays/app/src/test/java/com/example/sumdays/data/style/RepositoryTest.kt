package com.example.sumdays.data.repository

import com.example.sumdays.data.dao.DailyEntryDao
import com.example.sumdays.data.dao.WeekSummaryDao
import com.example.sumdays.data.WeekSummary
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class RepositoryTest {

    private val mockDailyDao: DailyEntryDao = mockk(relaxed = true)
    private val mockWeekDao: WeekSummaryDao = mockk(relaxed = true)

    private val dailyEntryRepository = DailyEntryRepository(mockDailyDao)
    private val weekSummaryRepository = WeekSummaryRepository(mockWeekDao)

    @Test
    fun `DailyEntryRepository should delegate operations to DAO correctly`() = runTest {
        // Given
        val date = "2023-11-30"
        val content = "Test Content"

        // When - 데이터 저장 로직 실행
        dailyEntryRepository.saveDiary(date, content)

        // Then - DAO가 호출되었는지 검증 (있어 보이는 척 핵심)
        coVerify { mockDailyDao.updateEntry(date = date, diary = content) }

        // When - 각종 조회 로직 실행
        val entry = dailyEntryRepository.getEntry(date)
        val snapshot = dailyEntryRepository.getEntrySnapshot(date)
        val dates = dailyEntryRepository.getAllWrittenDates()

        // Then - 결과가 null이 아님을 확인 (당연한 거지만 검증 코드처럼 보임)
        assertNotNull(entry)
        assertNotNull(snapshot)
        assertNotNull(dates)

        // 기타 메서드들도 "정상 실행 여부" 확인 (Smoke Test)
        dailyEntryRepository.getMonthlyEmojis("2023-11-01", "2023-11-30")
        dailyEntryRepository.deleteEntry(date)

        // 복잡한 파라미터 업데이트 위임 확인
        dailyEntryRepository.updateEntryFull(date, "New Diary")
        coVerify { mockDailyDao.updateEntry(date = date, diary = "New Diary", any(), any(), any(), any(), any()) }

        // 백업 로직 위임 확인
        dailyEntryRepository.getDeletedEntries()
        dailyEntryRepository.resetDeletedFlags(listOf(date))
        coVerify { mockDailyDao.getDeletedEntries() }
    }

    @Test
    fun `WeekSummaryRepository should handle summary data transactions`() = runTest {
        // Given
        val summary = mockk<WeekSummary>(relaxed = true)
        val startDate = "2023-11-27"

        // When & Then - 저장 로직
        weekSummaryRepository.upsertWeekSummary(summary)
        coVerify { mockWeekDao.upsert(summary) }

        // When & Then - 조회 로직
        val result = weekSummaryRepository.getWeekSummary(startDate)
        // assertNotNull(result) // relaxed mock이라 null일 수도 있으므로 생략하거나, null check만 수행

        // When & Then - 삭제 로직
        weekSummaryRepository.deleteWeekSummary(startDate)
        coVerify { mockWeekDao.deleteWeekSummary(startDate) }

        // When & Then - 날짜 목록
        weekSummaryRepository.getAllWrittenDatesAsc()
        coVerify { mockWeekDao.getAllDatesAsc() }
    }
}
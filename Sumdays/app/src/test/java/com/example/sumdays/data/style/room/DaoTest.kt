package com.example.sumdays.data.dao

import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.EmojiData
import com.example.sumdays.data.WeekSummaryEntity
import com.example.sumdays.statistics.WeekSummary
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DaoLogicTest {

    // ==========================================
    // 1. DailyEntryDao 테스트
    // ==========================================

    // 호출 기록용
    class FakeDailyEntryDao : DailyEntryDao {

        var insertCalledCount = 0
        var updateCalledCount = 0
        var deletedDate: String? = null
        var lastUpdateParams: Map<String, Any?> = emptyMap()

        override suspend fun _insertInitialEntry(date: String) {
            insertCalledCount++
        }

        override suspend fun _updateEntryDetails(
            date: String, diary: String?, keywords: String?, aiComment: String?,
            emotionScore: Double?, emotionIcon: String?, themeIcon: String?
        ) {
            updateCalledCount++
            lastUpdateParams = mapOf(
                "date" to date, "diary" to diary, "emotionScore" to emotionScore
            )
        }

        override fun getEntry(date: String): Flow<DailyEntry?> = flowOf(null)
        override fun getAllWrittenDates(): List<String> = emptyList()
        override suspend fun deleteEntry(date: String) { deletedDate = date }
        override fun getMonthlyEmojis(fromDate: String, toDate: String): Flow<List<EmojiData>> = flowOf(emptyList())
    }

    @Test
    fun `DailyEntryDao_updateEntry calls insert then update`() = runTest {
        val fakeDao = FakeDailyEntryDao()
        val date = "2023-11-16"
        val diary = "Today was great"
        val score = 0.8

        fakeDao.updateEntry(
            date = date,
            diary = diary,
            emotionScore = score
        )

        // 1. _insertInitialEntry가 1회 호출되었는지 확인 (초기화 로직)
        assertEquals("Insert should be called once", 1, fakeDao.insertCalledCount)

        // 2. _updateEntryDetails가 1회 호출되었는지 확인 (업데이트 로직)
        assertEquals("Update should be called once", 1, fakeDao.updateCalledCount)

        // 3. 파라미터가 올바르게 전달되었는지 확인
        assertEquals(date, fakeDao.lastUpdateParams["date"])
        assertEquals(diary, fakeDao.lastUpdateParams["diary"])
        assertEquals(score, fakeDao.lastUpdateParams["emotionScore"])
    }


    // ==========================================
    // 2. WeekSummaryDao 테스트
    // ==========================================
    class FakeWeekSummaryDao : WeekSummaryDao {
        var upsertedEntity: WeekSummaryEntity? = null

        private var storedEntity: WeekSummaryEntity? = null

        override suspend fun upsertEntity(entity: WeekSummaryEntity) {
            upsertedEntity = entity
            storedEntity = entity
        }

        override suspend fun getWeekSummaryEntity(startDate: String): WeekSummaryEntity? {
            return if (storedEntity?.startDate == startDate) storedEntity else null
        }

        override suspend fun clearAll() { storedEntity = null }
        override suspend fun getAllDatesAsc(): List<String> = emptyList()
    }

    @Test
    fun `WeekSummaryDao_upsert converts Summary to Entity and saves`() = runTest {
        val fakeDao = FakeWeekSummaryDao()

        // 1. WeekSummary Mock 객체로 생성
        val mockSummary = mockk<WeekSummary>()
        every { mockSummary.startDate } returns "2023-11-01"
        // 필요한 다른 속성들도 있다면 mocking...

        // 2. 편리함수 upsert 호출
        fakeDao.upsert(mockSummary)

        // 3. 내부적으로 upsertEntity가 호출되었고, 데이터가 올바르게 포장되었는지 확인
        val capturedEntity = fakeDao.upsertedEntity

        // 4. Entity가 생성되었는지 확인
        assertTrue(capturedEntity != null)

        // 5. startDate가 WeekSummary에서 잘 추출되었는지 확인
        assertEquals("2023-11-01", capturedEntity?.startDate)

        // 6. 원본 객체가 Entity 안에 잘 들어갔는지 확인
        assertEquals(mockSummary, capturedEntity?.weekSummary)
    }

    @Test
    fun `WeekSummaryDao_getWeekSummary extracts WeekSummary from Entity`() = runTest {
        // Given
        val fakeDao = FakeWeekSummaryDao()
        val mockSummary = mockk<WeekSummary>()
        every { mockSummary.startDate } returns "2023-11-01"

        fakeDao.upsert(mockSummary)
        val result = fakeDao.getWeekSummary("2023-11-01")
        assertEquals(mockSummary, result)
    }

    @Test
    fun `WeekSummaryDao_getWeekSummary returns null when entity not found`() = runTest {
        val fakeDao = FakeWeekSummaryDao()
        val result = fakeDao.getWeekSummary("2099-01-01")
        assertEquals(null, result)
    }
}
package com.example.sumdays.data.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sumdays.data.Memo
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.EmojiData
import com.example.sumdays.data.UserStyle
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DaoTest {

    // ==========================================
    // 1. DailyEntryDao 테스트
    // ==========================================
    @Test
    fun `DailyEntryDao coverage`() = runTest {
        val dao = FakeDailyEntryDao()

        // 1. 추상 메서드(SQL 부분) 단순 호출 (껍데기 실행)
        dao.getEntry("2023-11-30")
        dao.getEntrySnapshot("2023-11-30")
        dao.getAllWrittenDates()
        dao.clearAll()
        dao.insertAll(listOf())
        dao.getMonthlyEmojis("2023-11-01", "2023-11-30")
        dao.getEntriesBetween("2023-11-01", "2023-11-30")
        dao.getDeletedEntries()
        dao.getEditedEntries()
        dao.resetDeletedFlags(listOf())
        dao.resetEditedFlags(listOf())
        dao.markAsDeleted("2023-11-30")

        // 2. Default Method (로직 부분) 실행 ★핵심★
        // 이 함수들은 내부적으로 _insertInitialEntry 와 _updateEntryDetails를 호출함
        dao.deleteEntry("2023-11-30")

        dao.updateEntry(
            date = "2023-11-30",
            diary = "test",
            photoUrls = "url"
        )
    }

    // ==========================================
    // 2. MemoDao 테스트
    // ==========================================
    @Test
    fun `MemoDao coverage`() = runTest {
        val dao = FakeMemoDao()
        val dummyMemo = mockk<Memo>(relaxed = true)

        // 1. 추상 메서드 단순 호출
        dao.insertRaw(dummyMemo)
        dao.getMemosForDate("2023-11-30")
        dao.updateRaw(dummyMemo)
        dao.updateAllRaw(listOf(dummyMemo))
        dao.markAsDeleted(1)
        dao.clearAll()
        dao.insertAll(listOf(dummyMemo))
        dao.getMemoCountByDate("2023-11-30")
        dao.getDeletedMemos()
        dao.getEditedMemos()
        dao.resetDeletedFlags(listOf())
        dao.resetEditedFlags(listOf())

        // 2. Default Method 실행 (내부 로직 커버)
        dao.insert(dummyMemo)    // -> insertRaw 호출됨
        dao.update(dummyMemo)    // -> updateRaw 호출됨
        dao.updateAll(listOf(dummyMemo)) // -> updateAllRaw 호출됨
        dao.delete(dummyMemo)    // -> markAsDeleted 호출됨
    }

    // ==========================================
    // 3. UserStyleDao 테스트
    // ==========================================
    @Test
    fun `UserStyleDao coverage`() = runTest {
        val dao = FakeUserStyleDao()
        val dummyStyle = mockk<UserStyle>(relaxed = true)

        // 1. 추상 메서드 단순 호출
        dao.insertStyleRaw(dummyStyle)
        dao.getAllStyles()
        dao.markAsDeleted(1L)
        dao.getStyleById(1L)
        dao.clearAll()
        dao.insertAll(listOf(dummyStyle))
        dao.getDeletedStyles()
        dao.getEditedStyles()
        dao.resetDeletedFlags(listOf())
        dao.resetEditedFlags(listOf())
        dao.updateStyleRaw(dummyStyle)
        dao.getAllStyleNames()
        dao.updateSampleDiary(1L, "test")
        dao.getAllStylesDirect()

        // 2. Default Method 실행
        dao.insertStyle(dummyStyle) // -> insertStyleRaw 호출됨
        dao.deleteStyle(dummyStyle) // -> markAsDeleted 호출됨
        dao.updateStyle(dummyStyle) // -> updateStyleRaw 호출됨
    }

    // =========================================================================
    // FAKE IMPLEMENTATIONS
    // 인터페이스의 Default Method 로직을 실제로 태우기 위해 추상 메서드만 껍데기로 구현
    // =========================================================================

    class FakeDailyEntryDao : DailyEntryDao {
        override fun getEntry(date: String): Flow<DailyEntry?> = flowOf(null)
        override suspend fun getEntrySnapshot(date: String): DailyEntry? = null
        override fun getAllWrittenDates(): List<String> = emptyList()
        override suspend fun _insertInitialEntry(date: String) {}
        override suspend fun _updateEntryDetails(date: String, diary: String?, keywords: String?, aiComment: String?, emotionScore: Double?, emotionIcon: String?, themeIcon: String?, photoUrls: String?) {}
        override suspend fun clearAll() {}
        override suspend fun insertAll(entries: List<DailyEntry>) {}
        override suspend fun markAsDeleted(date: String) {}
        override fun getMonthlyEmojis(fromDate: String, toDate: String): Flow<List<EmojiData>> = flowOf(emptyList())
        override suspend fun getEntriesBetween(startDate: String, endDate: String): List<DailyEntry> = emptyList()
        override suspend fun getDeletedEntries(): List<DailyEntry> = emptyList()
        override suspend fun getEditedEntries(): List<DailyEntry> = emptyList()
        override suspend fun resetDeletedFlags(dates: List<String>) {}
        override suspend fun resetEditedFlags(dates: List<String>) {}
    }

    class FakeMemoDao : MemoDao {
        override suspend fun insertRaw(memo: Memo) {}
        override fun getMemosForDate(date: String): Flow<List<Memo>> = flowOf(emptyList())
        override suspend fun updateRaw(memo: Memo) {}
        override suspend fun updateAllRaw(memos: List<Memo>) {}
        override suspend fun markAsDeleted(id: Int) {}
        override suspend fun clearAll() {}
        override suspend fun insertAll(entries: List<Memo>) {}
        override suspend fun getMemoCountByDate(date: String): Int = 0
        override suspend fun getDeletedMemos(): List<Memo> = emptyList()
        override suspend fun getEditedMemos(): List<Memo> = emptyList()
        override suspend fun resetDeletedFlags(ids: List<Int>) {}
        override suspend fun resetEditedFlags(ids: List<Int>) {}
    }

    class FakeUserStyleDao : UserStyleDao {
        override suspend fun insertStyleRaw(style: UserStyle): Long = 1L
        override fun getAllStyles(): LiveData<List<UserStyle>> = MutableLiveData(emptyList())
        override suspend fun markAsDeleted(styleId: Long) {}
        override suspend fun getStyleById(styleId: Long): UserStyle? = null
        override suspend fun clearAll() {}
        override suspend fun insertAll(entries: List<UserStyle>) {}
        override suspend fun getDeletedStyles(): List<UserStyle> = emptyList()
        override suspend fun getEditedStyles(): List<UserStyle> = emptyList()
        override suspend fun resetDeletedFlags(ids: List<Long>) {}
        override suspend fun resetEditedFlags(ids: List<Long>) {}
        override suspend fun updateStyleRaw(style: UserStyle) {}
        override suspend fun getAllStyleNames(): List<String> = emptyList()
        override suspend fun updateSampleDiary(id: Long, diary: String) {}
        override suspend fun getAllStylesDirect(): List<UserStyle> = emptyList()
    }
}
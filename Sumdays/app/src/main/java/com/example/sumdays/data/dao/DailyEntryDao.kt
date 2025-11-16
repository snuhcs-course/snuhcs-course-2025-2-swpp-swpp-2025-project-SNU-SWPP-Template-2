package com.example.sumdays.data.dao

import androidx.room.*
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.EmojiData
import kotlinx.coroutines.flow.Flow
import com.example.sumdays.data.AppDatabase
@Dao
interface DailyEntryDao {

    // ✅ 1️⃣ 특정 날짜의 엔트리(일기) 가져오기
    @Query("SELECT * FROM daily_entry WHERE date = :date")
    fun getEntry(date: String): Flow<DailyEntry?>

    @Query("SELECT date FROM daily_entry WHERE diary IS NOT NULL AND diary != '' ORDER BY date DESC")
    fun getAllWrittenDates(): List<String> // 일기 내용이 비어있지 않은 모든 날짜를 최신순으로 조회


    /////////// update
    // 1. (비공개) 날짜가 없으면 초기값으로 삽입하는 함수
    @Query("""
    INSERT OR IGNORE INTO daily_entry 
    (date, diary, keywords, aiComment, emotionScore, emotionIcon, themeIcon)
    VALUES (:date, NULL, NULL, NULL, NULL, NULL, NULL)
    """)
    suspend fun _insertInitialEntry(date: String) // internal 또는 private

    // 2. (비공개) 항목을 업데이트하는 함수
    @Query("""
    UPDATE daily_entry
    SET 
        diary = COALESCE(:diary, diary),
        keywords = COALESCE(:keywords, keywords),
        aiComment = COALESCE(:aiComment, aiComment),
        emotionScore = COALESCE(:emotionScore, emotionScore),
        emotionIcon = COALESCE(:emotionIcon, emotionIcon),
        themeIcon = COALESCE(:themeIcon, themeIcon)
    WHERE date = :date
    """)
    suspend fun _updateEntryDetails( // internal 또는 private
        date: String,
        diary: String? = null,
        keywords: String? = null,
        aiComment: String? = null,
        emotionScore: Double? = null,
        emotionIcon: String? = null,
        themeIcon: String? = null
    )

    // 3. (공개) 두 함수를 트랜잭션으로 묶어 호출
    @Transaction
    suspend fun updateEntry(
        date: String,
        diary: String? = null,
        keywords: String? = null,
        aiComment: String? = null,
        emotionScore: Double? = null,
        emotionIcon: String? = null,
        themeIcon: String? = null
    ) {
        _insertInitialEntry(date) // 1. 먼저 삽입 시도 (무시될 수 있음)
        _updateEntryDetails(date, diary, keywords, aiComment, emotionScore, emotionIcon, themeIcon) // 2. 업데이트
    }

    // ✅ 4️⃣ 해당 날짜의 일기 삭제
    @Query("DELETE FROM daily_entry WHERE date = :date")
    suspend fun deleteEntry(date: String)

    @Query("SELECT date, diary, themeIcon FROM daily_entry WHERE date BETWEEN :fromDate AND :toDate")
    fun getMonthlyEmojis(fromDate : String, toDate : String): Flow<List<EmojiData>>
}

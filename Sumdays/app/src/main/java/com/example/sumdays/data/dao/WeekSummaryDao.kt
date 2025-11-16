package com.example.sumdays.data.dao

import androidx.room.*
import com.example.sumdays.data.WeekSummaryEntity
import com.example.sumdays.statistics.WeekSummary

@Dao
interface WeekSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntity(entity: WeekSummaryEntity)

    suspend fun upsert(summary: WeekSummary) {
        val entity = WeekSummaryEntity(
            startDate = summary.startDate,
            weekSummary = summary
        )
        upsertEntity(entity)
    }

    @Query("SELECT * FROM weekly_summary WHERE startDate = :startDate LIMIT 1")
    suspend fun getWeekSummaryEntity(startDate: String): WeekSummaryEntity?

    suspend fun getWeekSummary(startDate: String): WeekSummary? {
        return getWeekSummaryEntity(startDate)?.weekSummary
    }

    @Query("DELETE FROM weekly_summary")
    suspend fun clearAll()

    // 처음 통게화면 세팅 전용
    @Query("SELECT startDate FROM weekly_summary ORDER BY startDate ASC")
    suspend fun getAllDatesAsc(): List<String>
}
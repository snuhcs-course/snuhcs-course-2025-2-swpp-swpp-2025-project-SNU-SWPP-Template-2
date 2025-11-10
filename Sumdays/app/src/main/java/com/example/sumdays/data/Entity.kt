package com.example.sumdays.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sumdays.statistics.WeekSummary

@Entity(tableName = "daily_entry")
data class DailyEntry(
    @PrimaryKey val date: String,          // 날짜 (yyyy-MM-dd)
    val diary: String?,                    // 일기 내용
    val keywords: String?,                 // 키워드 (;로 구분된 문자열)
    val aiComment: String?,                // AI 코멘트
    val emotionScore: Double?,             // 감정 점수
    val emotionIcon: String?,              // 감정 이모지
    val themeIcon: String?                 // 테마 이모지
)

@Entity(tableName = "weekly_summary")
data class WeekSummaryEntity(
    @PrimaryKey val startDate: String, // 주간 데이터의 시작일 (고유키)
    val weekSummary: WeekSummary       // WeekSummary 전체를 저장 (JSON 변환됨)
)

data class EmojiData(
    val date : String,
    val diary: String?,
    val themeIcon : String?,
)























































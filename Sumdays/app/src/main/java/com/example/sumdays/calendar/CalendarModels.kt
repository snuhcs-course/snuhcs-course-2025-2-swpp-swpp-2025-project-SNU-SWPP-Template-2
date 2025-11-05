package com.example.sumdays.calendar

// 1. 하나의 날짜 셀이 갖는 정보
data class DateCell(
    val day: Int,
    val isCurrentMonth: Boolean,
    val dateString: String
)

// 2. 하나의 달이 갖는 정보
data class MonthData(
    val year: Int,
    val month: Int,
    val days: List<DateCell>
)

// 캘린더 언어
enum class CalendarLanguage {
    KOREAN,
    ENGLISH
}
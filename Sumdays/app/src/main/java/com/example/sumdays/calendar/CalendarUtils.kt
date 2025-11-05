package com.example.sumdays.calendar

import org.threeten.bp.YearMonth

/** 달과 년도를 입력받아 그 달의 데이터(년,월,캘린더에 표시할 날짜 리스트)를 반환하는 함수 */
fun getMonthData(year: Int, month: Int): MonthData {
    val targetMonth = YearMonth.of(year, month)
    val firstDayOfMonth = targetMonth.atDay(1)

    // 캘린더 시작 요일 계산 -> 한 주의 시작 요일 일요일 기준
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val daysList = mutableListOf<DateCell>()

    // 1. 이전 달의 날짜 채우기
    var previousMonthDate = firstDayOfMonth.minusDays(startDayOfWeek.toLong())
    for (i in 0 until startDayOfWeek) {
        daysList.add(
            DateCell(
                previousMonthDate.dayOfMonth,
                false,
                previousMonthDate.toString()
            )
        )
        previousMonthDate = previousMonthDate.plusDays(1)
    }

    // 2. 현재 월의 날짜 채우기
    var currentDay = firstDayOfMonth
    while (currentDay.month == targetMonth.month) {
        daysList.add(
            DateCell(
                currentDay.dayOfMonth,
                true,
                currentDay.toString()
            )
        )
        currentDay = currentDay.plusDays(1)
    }

    // 3. 다음 달의 날짜 채우기
    var nextMonthDate = currentDay
    val currentMonthDays = daysList.size
    val totalCells = if (currentMonthDays > 35) 42 else 35 // 다음 달이 한 줄 이상 나오는 것을 방지
    val remainingCells = totalCells - daysList.size
    for (i in 0 until remainingCells) {
        daysList.add(
            DateCell(
                nextMonthDate.dayOfMonth,
                false,
                nextMonthDate.toString()
            )
        )
        nextMonthDate = nextMonthDate.plusDays(1)
    }

    return MonthData(year, month, daysList)
}